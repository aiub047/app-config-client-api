package com.mak.AWS.AppConfigClient.services.impls;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mak.AWS.AppConfigClient.beans.*;
import com.mak.AWS.AppConfigClient.beans.request.FeatureFlagRequestBean;
import com.mak.AWS.AppConfigClient.beans.response.AppConfigResponseBean;
import com.mak.AWS.AppConfigClient.beans.response.ErrorDetailsBean;
import com.mak.AWS.AppConfigClient.services.AppConfigService;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.appconfig.AppConfigClient;
import software.amazon.awssdk.services.appconfig.model.*;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * @author Mohammad Aiub Khan
 * Created: 1/20/2024
 */

@Service
public class AppConfigServiceImpl implements AppConfigService {
    AppConfigClient appConfigClient;
    AppConfigClient appConfigDataClient;

    public AppConfigServiceImpl(AppConfigClient appConfigClient, AppConfigClient appConfigDataClient) {
        this.appConfigClient = appConfigClient;
        this.appConfigDataClient = appConfigDataClient;
    }

    @Override
    public AppConfigResponseBean getAllApplications() {
        List<ApplicationBean> listApplicationBean = new ArrayList<>();
        ListApplicationsResponse response = appConfigClient.listApplications(ListApplicationsRequest.builder().build());
        List<Application> applications = response.items();
        for (Application apps : applications)
            listApplicationBean.add(new ApplicationBean(apps.id(), apps.name(), apps.description()));

        AppConfigResponseBean responseBean = new AppConfigResponseBean();
        responseBean.setList(listApplicationBean);
        responseBean.setStatus(Constants.SUCCESS);
        return responseBean;
    }

    @Override
    public AppConfigResponseBean getProfilesByApplicationId(String applicationId) {
        AppConfigResponseBean appConfigResponseBean = new AppConfigResponseBean();
        List<ProfileBean> profiles = new ArrayList<>();

        ListConfigurationProfilesRequest request = ListConfigurationProfilesRequest.builder().applicationId(applicationId).build();

        ListConfigurationProfilesResponse response = appConfigClient.listConfigurationProfiles(request);
        List<ConfigurationProfileSummary> configs = response.items();

        for (ConfigurationProfileSummary summary : configs)
            profiles.add(new ProfileBean(summary.applicationId(), summary.id(), summary.name(), summary.locationUri(), summary.type()));

        appConfigResponseBean.setList(profiles);
        appConfigResponseBean.setStatus(Constants.SUCCESS);

        return appConfigResponseBean;
    }

    @Override
    public AppConfigResponseBean getEnvironmentsByApplicationId(String applicationId) {
        AppConfigResponseBean appConfigResponseBean = new AppConfigResponseBean();
        ListEnvironmentsRequest listEnvironmentsRequest = ListEnvironmentsRequest.builder().applicationId(applicationId).build();
        ListEnvironmentsResponse listEnvironmentsResponse = appConfigClient.listEnvironments(listEnvironmentsRequest);
        List<Environment> environments = listEnvironmentsResponse.items();

        List<EnvironmentBean> environmentBeans = new ArrayList<>();
        for (Environment env : environments)
            environmentBeans.add(new EnvironmentBean(env.applicationId(), env.id(), env.name(), env.description(), env.state().toString()));

        return new AppConfigResponseBean(environmentBeans, Constants.SUCCESS);
    }

    @Override
    public AppConfigResponseBean getFeatureFlagsByAppProfileEnvId(String applicationId, String profileId, String environmentId) {
        AppConfigResponseBean appConfigResponseBean = new AppConfigResponseBean();
        try {
            List<HostedConfigurationVersionSummary> versions = appConfigClient.listHostedConfigurationVersions(ListHostedConfigurationVersionsRequest.builder().applicationId(applicationId).configurationProfileId(profileId).build()).items();

            Integer latestVersionNumber = versions.stream().max(Comparator.comparing(HostedConfigurationVersionSummary::versionNumber)).map(HostedConfigurationVersionSummary::versionNumber).orElseThrow(() -> new RuntimeException("No versions found"));

            GetHostedConfigurationVersionResponse ver = appConfigClient.getHostedConfigurationVersion(GetHostedConfigurationVersionRequest.builder().configurationProfileId(profileId).applicationId(applicationId).versionNumber(latestVersionNumber).build());

            String jsonString = ver.content().asUtf8String();

            System.out.println(jsonString);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonString);

            JsonNode flagsNode = rootNode.path("flags");
            JsonNode valuesNode = rootNode.path("values");

            String version = rootNode.path("version").asText();

            ArrayNode resultArray = mapper.createArrayNode();

            for (Iterator<String> it = flagsNode.fieldNames(); it.hasNext(); ) {
                String flagName = it.next();

                JsonNode flagNode = flagsNode.path(flagName);
                JsonNode valueNode = valuesNode.path(flagName);
                JsonNode attributesNode = flagNode.path("attributes");

                ObjectNode newNode = mapper.createObjectNode();
                ObjectNode newAttributeNode = mapper.createObjectNode();

                newNode.put("flag_key", flagName);
                newNode.put("flag_name", flagNode.path("name").asText());
                newNode.put("version", latestVersionNumber);
                newNode.put("enabled", valueNode.path("enabled").asBoolean());
                newNode.put("last_updated", flagNode.path("_updatedAt").asText());
                newNode.put("created", flagNode.path("_createdAt").asText());

                // Add attributes and their values to the response
                for (Iterator<String> attributesIt = attributesNode.fieldNames(); attributesIt.hasNext(); ) {
                    String attributeName = attributesIt.next();
                    JsonNode attributeValue = valueNode.path(attributeName);
                    JsonNode attributeType = attributesNode.path(attributeName).path("constraints").path("type");
                    String arrType;
                    String realType = attributeType.textValue();
                    if (attributeType.toString().equals("\"array\"")) {
                        arrType = attributeValue.get(0).getNodeType().toString().toLowerCase();
                        realType = arrType + " array";
                    }

                    ObjectNode attributeInfoNode = mapper.createObjectNode();
                    attributeInfoNode.put("value", attributeValue);
                    attributeInfoNode.put("type", realType);

                    newAttributeNode.set(attributeName, attributeInfoNode);
                }

                newNode.set("attributes", newAttributeNode);

                resultArray.add(newNode);
            }

            appConfigResponseBean.setList(Arrays.asList(resultArray));
            appConfigResponseBean.setStatus(Constants.SUCCESS);

        } catch (Exception e) {
            appConfigResponseBean.setErrorDetails(new ErrorDetailsBean("error", e.getLocalizedMessage()));
            appConfigResponseBean.setStatus(Constants.FAILED);
        }
        return appConfigResponseBean;
    }


    @Override
    public AppConfigResponseBean updateFeatureFlags(FeatureFlagRequestBean featureFlagRequestBean) {
        String applicationId = featureFlagRequestBean.getApplicationId();
        String profileId = featureFlagRequestBean.getProfileId();
        String environmentId = featureFlagRequestBean.getEnvironmentId();

        AppConfigResponseBean appConfigResponseBean = new AppConfigResponseBean();

        try {
            List<HostedConfigurationVersionSummary> versions = appConfigClient.listHostedConfigurationVersions(ListHostedConfigurationVersionsRequest.builder().applicationId(applicationId).configurationProfileId(profileId).build()).items();

            Integer latestVersion = versions.stream().max(Comparator.comparing(HostedConfigurationVersionSummary::versionNumber)).map(HostedConfigurationVersionSummary::versionNumber).orElseThrow(() -> new RuntimeException("No version found"));

            ListDeploymentsRequest request = ListDeploymentsRequest.builder().applicationId(applicationId).environmentId(environmentId).build();

            ListDeploymentsResponse response = appConfigClient.listDeployments(request);

            DeploymentSummary latestDeployment = response.items().get(0);

            System.out.println("Latest deployment number: " + latestDeployment.deploymentNumber());

            GetDeploymentRequest getDeploymentRequest = GetDeploymentRequest.builder().applicationId(applicationId).environmentId(environmentId).deploymentNumber(latestDeployment.deploymentNumber()).build();

            GetDeploymentResponse getDeploymentResponse = appConfigClient.getDeployment(getDeploymentRequest);

            System.out.println("Deployment status: " + getDeploymentResponse);

            if (!response.items().get(0).state().toString().equals("COMPLETE")) {
                appConfigResponseBean.setData("Previous deployment is still in progress, please try after " + response.items().get(0).finalBakeTimeInMinutes() + " minutes");
                throw new Exception("Previous deployment is in progress");
            }

            GetHostedConfigurationVersionResponse ver = appConfigClient.getHostedConfigurationVersion(GetHostedConfigurationVersionRequest.builder().configurationProfileId(profileId).applicationId(applicationId).versionNumber(latestVersion).build());

            String jsonString = ver.content().asUtf8String();
            ObjectMapper mapper = new ObjectMapper();

            JsonNode root = mapper.readTree(jsonString);

            ObjectNode values = (ObjectNode) root.get("values");
            ObjectNode valuesCopy = values.deepCopy();

            ObjectNode flagsNode = (ObjectNode) root.get("flags");

            for (FlagBean flag : featureFlagRequestBean.getFlags()) {
                //ObjectNode flagNode = (ObjectNode) values.get(flag.getFlagKey());
                ObjectNode flagNode = (ObjectNode) valuesCopy.get(flag.getFlagKey());
                flagNode.put("enabled", flag.getEnabled());

                for (AttributeBean att : flag.getAttributes()) {
                    String flagKey = flag.getFlagKey();
                    String attKey = att.getKey();
                    String type = flagsNode.get(flagKey).get("attributes").get(attKey).get("constraints").get("type").toString();

                    if (type.equals("\"number\"")) {
                        //For number
                        flagNode.put(att.getKey(), Integer.valueOf(Objects.requireNonNullElse(att.getValue(), "-1")));
                    } else if (type.equals("\"array\"")) {
                        if (att.getType().equals("string array")) {
                            //String array
                            flagNode.put(att.getKey(), new ObjectMapper().valueToTree(att.getValue().split(",\\s*")));
                        } else {
                            //Number array
                            int[] numArray = Arrays.stream(att.getValue().split(",\\s*")).mapToInt(Integer::parseInt).toArray();
                            flagNode.put(att.getKey(), new ObjectMapper().valueToTree(numArray));
                        }
                    } else {
                        //For string
                        flagNode.put(att.getKey(), att.getValue());
                    }
                }
            }

            ((ObjectNode) root).set("values", valuesCopy);

            String content = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            String contentType = "application/json";
            String description = "Updated version of hosted configuration for feature flag";
            ByteBuffer encodedContent = ByteBuffer.wrap(content.getBytes());

            CreateHostedConfigurationVersionRequest createRequest = CreateHostedConfigurationVersionRequest.builder().applicationId(applicationId).configurationProfileId(profileId).contentType(contentType).content(SdkBytes.fromByteBuffer(encodedContent)).description(description).build();

            CreateHostedConfigurationVersionResponse createResponse = appConfigClient.createHostedConfigurationVersion(createRequest);
            System.out.println("Hosted configuration version updated successfully with version number: " + createResponse.versionNumber());
            appConfigResponseBean.setData("Hosted configuration version updated successfully with version number: " + createResponse.versionNumber());

            String configurationVersion = String.valueOf(createResponse.versionNumber());

            StartDeploymentRequest startDeploymentRequest = StartDeploymentRequest.builder().applicationId(applicationId).configurationProfileId(profileId).environmentId(environmentId).configurationVersion(configurationVersion).deploymentStrategyId("AppConfig.AllAtOnce").build();
            appConfigClient.startDeployment(startDeploymentRequest);

            appConfigResponseBean.setStatus(Constants.SUCCESS);

        } catch (Exception e) {
            appConfigResponseBean.setData("Error: " + e.getMessage());
            appConfigResponseBean.setStatus(Constants.FAILED);
        }
        return appConfigResponseBean;
    }

}
