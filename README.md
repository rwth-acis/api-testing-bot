<h1 align="center">
  <img src="https://raw.githubusercontent.com/rwth-acis/api-testing-bot/main/.github/images/logo.png" width="120px"/><br/>
  API Testing Bot
</h1>

<p align="center">
<img src="https://github.com/rwth-acis/api-testing-bot/workflows/Java%20CI%20with%20Gradle/badge.svg?branch=main"/>
</p>

## ⚙️ Setup/Usage

First, set up the bot in the [Social-Bot-Framework](https://github.com/rwth-acis/Social-Bot-Framework).
Both the bot model and the NLU training data can be found in the `bot-model` directory.
The bot uses a backend service which is included in this repository.
The easiest way to use the service is to build (or pull) the Docker image and run the service as a container.
The bot can be used in different ways and on different platforms:

### 1. Usage with CAE & RocketChat

If the bot is a member of a RocketChat channel that is linked to a CAE project, test cases can be modeled via chat.
The bot can be triggered by sending a message such as "model a test".
As soon as the modeling is completed, the test case is forwarded to the CAE and proposed there in the Test Editor.

Dependencies:

- [las2peer-project-service](https://github.com/rwth-acis/las2peer-project-service)
- [CAE](https://github.com/rwth-acis/CAE)

### 2. Usage on GitHub

The bot may be used within GitHub repositories by connecting it to a GitHub app.
Therefore, the app id and a private key are required.
Please note that the private key needs to be converted from PKCS#1 to PKCS#8.
In the bot model, the `Authentication Token` of the GitHub issue or pull request messenger needs to be set to `[App Id]:[App Private Key]`.
Use the private key in PKCS#8 format, but remove the first and last line before copying it.
Also, set the webhook URL of the app to `.../apitestingbot/github/webhook/{gitHubAppId}`.

Then, the bot can be triggered within issues/pull requests by sending a message such as "model a test".
Once a test case has been modeled, the bot generates Java JUnit test code and comments it.

In pull requests, the bot may also propose spec-based test cases if it can access the developed service's OpenAPI documentation.
Read the [repository adjustment guide](repo_adjustment_guide.md) for more information.

Dependencies:

- [CAE-Code-Generation-Service](https://github.com/rwth-acis/CAE-Code-Generation-Service)
- [api-test-gen-service](https://github.com/rwth-acis/api-test-gen-service)


## 🐳 Docker Environment Variables

Depending on the use case different environment variables are required:

| Environment Variable     | Description                                                                                  | Required?           |
|--------------------------|----------------------------------------------------------------------------------------------|---------------------|
| `BOT_MANAGER_URL`        | REST API URL of Bot Manager Service ending with `/SBFManager`.                               | Yes                 |
| `CODEX_API_TOKEN`        | Token for accessing the OpenAI/Codex API.                                                    | Yes                 |
| `CAE_BACKEND_URL`        | REST API URL of CAE Model Persistence Service ending with `/CAE`.                            | Only in 1. Use-case |
| `GITHUB_APP_ID`          | Id of GitHub app that the bot should use.                                                    | Only in 2. Use-case |
| `GITHUB_APP_PRIVATE_KEY` | Private key of GitHub app that the bot should use (already needs to be converted to PKCS#8). | Only in 2. Use-case |
