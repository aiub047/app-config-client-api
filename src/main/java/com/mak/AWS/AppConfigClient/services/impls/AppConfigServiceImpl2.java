package com.mak.AWS.AppConfigClient.services.impls;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mak.AWS.AppConfigClient.beans.Constants;
import com.mak.AWS.AppConfigClient.beans.FlagBean;
import com.mak.AWS.AppConfigClient.beans.request.FeatureFlagRequestBean;
import com.mak.AWS.AppConfigClient.beans.response.AppConfigResponseBean;
import com.mak.AWS.AppConfigClient.beans.response.ErrorDetailsBean;
import com.mak.AWS.AppConfigClient.services.AppConfigService;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.appconfig.AppConfigClient;
import software.amazon.awssdk.services.appconfig.model.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * @author Mohammad Aiub Khan
 * Created: 1/21/2024
 */
public class AppConfigServiceImpl2 implements AppConfigService {
    AppConfigClient appConfigClient;
    AppConfigClient appConfigDataClient;


    @Override
    public AppConfigResponseBean getAllApplications() {
        return null;
    }

    @Override
    public AppConfigResponseBean getProfilesByApplicationId(String applicationId) {
        return null;
    }

    @Override
    public AppConfigResponseBean getEnvironmentsByApplicationId(String applicationId) {
        return null;
    }

    @Override
    public AppConfigResponseBean getFeatureFlagsByAppProfileEnvId(String applicationId, String profileId, String environmentId) {
        AppConfigResponseBean appConfigResponseBean = new AppConfigResponseBean();
        try {
            List<HostedConfigurationVersionSummary> versions = appConfigClient.listHostedConfigurationVersions(
                            ListHostedConfigurationVersionsRequest.builder().applicationId(applicationId).configurationProfileId(profileId).build())
                    .items();

            Integer latestVersionNumber = versions.stream().max(Comparator.comparing(HostedConfigurationVersionSummary::versionNumber))
                    .map(HostedConfigurationVersionSummary::versionNumber).orElseThrow(() -> new RuntimeException("No versions found"));

            GetHostedConfigurationVersionResponse ver = appConfigClient.getHostedConfigurationVersion(
                    GetHostedConfigurationVersionRequest.builder().configurationProfileId(profileId).applicationId(applicationId).versionNumber(latestVersionNumber).build());

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

                // Add attributes and their values to the response
                for (Iterator<String> attributesIt = attributesNode.fieldNames(); attributesIt.hasNext(); ) {
                    String attributeName = attributesIt.next();
                    JsonNode attributeValue = valueNode.path(attributeName);

                    newAttributeNode.put(attributeName, attributeValue);
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
        AppConfigResponseBean appConfigResponseBean = new AppConfigResponseBean();

        try {
            List<HostedConfigurationVersionSummary> versions = appConfigClient.listHostedConfigurationVersions(ListHostedConfigurationVersionsRequest.builder().applicationId(featureFlagRequestBean.getApplicationId()).configurationProfileId(featureFlagRequestBean.getProfileId()).build()).items();

            Integer latestVersion = versions.stream().max(Comparator.comparing(HostedConfigurationVersionSummary::versionNumber)).map(HostedConfigurationVersionSummary::versionNumber).orElseThrow(() -> new RuntimeException("No version found"));

            ListDeploymentsRequest request = ListDeploymentsRequest.builder()
                    .applicationId(featureFlagRequestBean.getApplicationId())
                    .environmentId(featureFlagRequestBean.getEnvironmentId())
                    .build();

            ListDeploymentsResponse response = appConfigClient.listDeployments(request);

            DeploymentSummary latestDeployment = response.items().get(0);

            System.out.println("Latest deployment number: " + latestDeployment.deploymentNumber());

            GetDeploymentRequest getDeploymentRequest = GetDeploymentRequest.builder().applicationId(featureFlagRequestBean.getApplicationId())
                    .environmentId(featureFlagRequestBean.getEnvironmentId())
                    .deploymentNumber(latestDeployment.deploymentNumber())
                    .build();

            GetDeploymentResponse getDeploymentResponse = appConfigClient.getDeployment(getDeploymentRequest);

            System.out.println("Deployment status: " + getDeploymentResponse);

            if (!response.items().get(0).state().toString().equals("COMPLETE")) {
                appConfigResponseBean.setData("Previous deployment is still in progress, please try after " + response.items().get(0).finalBakeTimeInMinutes() + " minutes");
                throw new Exception("Previous deployment is in progress");
            }

            GetHostedConfigurationVersionResponse ver = appConfigClient.getHostedConfigurationVersion(GetHostedConfigurationVersionRequest.builder().configurationProfileId(featureFlagRequestBean.getProfileId()).applicationId(featureFlagRequestBean.getApplicationId()).versionNumber(latestVersion).build());

            String jsonString = ver.content().asUtf8String();
            ObjectMapper mapper = new ObjectMapper();

            JsonNode root = mapper.readTree(jsonString);

            ObjectNode values = (ObjectNode) root.get("values");

            for (FlagBean flag : featureFlagRequestBean.getFlags()) {
                ObjectNode flagNode = (ObjectNode) values.get(flag.getFlagKey());
                flagNode.put("enabled", flag.getEnabled());
            }

            String content = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            String contentType = "application/json";
            String description = "Updated version of hosted configuration for feature flag";
            ByteBuffer encodedContent = ByteBuffer.wrap(content.getBytes());

            CreateHostedConfigurationVersionRequest createRequest = CreateHostedConfigurationVersionRequest.builder().applicationId(featureFlagRequestBean.getApplicationId()).configurationProfileId(featureFlagRequestBean.getProfileId()).contentType(contentType).content(SdkBytes.fromByteBuffer(encodedContent)).description(description).build();

            CreateHostedConfigurationVersionResponse createResponse = appConfigClient.createHostedConfigurationVersion(createRequest);
            System.out.println("Hosted configuration version updated successfully with version number: " + createResponse.versionNumber());
            appConfigResponseBean.setData("Hosted configuration version updated successfully with version number: " + createResponse.versionNumber());

            String configurationVersion = String.valueOf(createResponse.versionNumber());

            StartDeploymentRequest startDeploymentRequest = StartDeploymentRequest.builder().applicationId(featureFlagRequestBean.getApplicationId()).configurationProfileId(featureFlagRequestBean.getProfileId()).environmentId(featureFlagRequestBean.getEnvironmentId()).configurationVersion(configurationVersion).deploymentStrategyId("AppConfig.AllAtOnce").build();
            appConfigClient.startDeployment(startDeploymentRequest);
            appConfigResponseBean.setStatus(Constants.SUCCESS);

        } catch (Exception e) {
            appConfigResponseBean.setData("Error: " + e.getMessage());
            appConfigResponseBean.setStatus(Constants.FAILED);
        }
        return appConfigResponseBean;
    }
}
