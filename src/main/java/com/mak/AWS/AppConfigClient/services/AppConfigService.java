package com.mak.AWS.AppConfigClient.services;

import com.mak.AWS.AppConfigClient.beans.request.FeatureFlagRequestBean;
import com.mak.AWS.AppConfigClient.beans.response.AppConfigResponseBean;

/**
 * @author Mohammad Aiub Khan
 * Created: 1/20/2024
 */
public interface AppConfigService {
    AppConfigResponseBean getAllApplications();

    AppConfigResponseBean getProfilesByApplicationId(String applicationId);

    AppConfigResponseBean getEnvironmentsByApplicationId(String applicationId);

    AppConfigResponseBean getFeatureFlagsByAppProfileEnvId(String applicationId, String profileId, String environmentId);

    AppConfigResponseBean updateFeatureFlags(FeatureFlagRequestBean featureFlagRequestBean);
}
