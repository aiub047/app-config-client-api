package com.mak.AWS.AppConfigClient.controllers;

import com.mak.AWS.AppConfigClient.beans.request.FeatureFlagRequestBean;
import com.mak.AWS.AppConfigClient.beans.response.AppConfigResponseBean;
import com.mak.AWS.AppConfigClient.services.AppConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.http.HttpResponse;

/**
 * @author Mohammad Aiub Khan
 * Created: 1/20/2024
 */

@RestController
@RequestMapping("/api/v1")
public class ApiController {
    AppConfigService appConfigService;

    public ApiController(AppConfigService appConfigService) {
        this.appConfigService = appConfigService;
    }

    @GetMapping("/greeting")
    public ResponseEntity<String> greetings() {
        return new ResponseEntity<>("Welcome to the AppConfigClient", HttpStatus.OK);
    }

    @GetMapping("/applications")
    public ResponseEntity<AppConfigResponseBean> getApplications() {
        return new ResponseEntity<>(appConfigService.getAllApplications(), HttpStatus.OK);
    }

    @GetMapping("/profiles/{appId}")
    public ResponseEntity<AppConfigResponseBean> getProfiles(@PathVariable("appId") String appId) {
        return new ResponseEntity<>(appConfigService.getProfilesByApplicationId(appId), HttpStatus.OK);
    }

    @GetMapping("/environments/{appId}")
    public ResponseEntity<AppConfigResponseBean> getEnvironments(@PathVariable("appId") String appId) {
        return new ResponseEntity<>(appConfigService.getEnvironmentsByApplicationId(appId), HttpStatus.OK);
    }

    @GetMapping("/flags/{appId}/{profId}/{envId}")
    public ResponseEntity<AppConfigResponseBean> getFeatherFlags(@PathVariable("appId") String appId, @PathVariable("profId") String profId, @PathVariable("envId") String envId) {
        return new ResponseEntity<>(appConfigService.getFeatureFlagsByAppProfileEnvId(appId, profId, envId), HttpStatus.OK);
    }

    @PostMapping("/flags")
    public ResponseEntity<AppConfigResponseBean> postFeatherFlags(@RequestBody FeatureFlagRequestBean featureFlagRequestBean) {
        return new ResponseEntity<>(appConfigService.updateFeatureFlags(featureFlagRequestBean), HttpStatus.OK);
    }
}
