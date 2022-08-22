# API Testing Bot: Repo Adjustment Guide

In order to use the API Testing Bot for proposing spec-based test cases in GitHub pull requests, a GitHub Actions workflow producing an OpenAPI documentation artifact is required.
The bot needs this to access the service's OpenAPI documentation and to analyze how it changes within pull requests.
Based on these changes, the bot tries to generate test cases.

## Prerequisites

This guide assumes that the developed service is a las2peer service following the structure of the las2peer-template-project and that the repository contains a gradle.yml workflow file similar to the one from the template-project.

## Adjustments

**Adjustments to the service test:**

During the service test, the service gets started and the OpenAPI documentation of the service is made available via the webconnector.
Inside the `startServer()` method of the test file, add the following lines at the end:

```java
// download swagger.json
InputStream in = new URL(connector.getHttpEndpoint() + "/" + mainPath + "swagger.json").openStream();
Files.copy(in, Paths.get("export/swagger.json"), StandardCopyOption.REPLACE_EXISTING);
```

This will download and store the OpenAPI documentation from the webconnector.
The download path (export/swagger.json) is later used in the workflow file when uploading the documentation as a workflow artifact.

**Adjustments to the workflow file:**

- The workflow needs to run on `push` and `pull_request` events for each branch (this is different to the workflow file from the template-project):

  ```yaml
  on: [push, pull_request]
  ```

- The swagger.json file needs to be uploaded as a workflow artifact. Therefore, add the following step (after the gradle build and test have been executed):

  ```yaml
  - uses: actions/upload-artifact@v3
      if: always()
      with:
        name: swagger.json
        path: template_project/export/swagger.json
  ```

  Replace `template_project` with the directory name for your service.
