# Contributing to Devscode

## Table of Contents

- [Contributing to Devscode](#contributing-to-devscode)
  - [Table of Contents](#table-of-contents)
- [❤️ Ways to Contribute](#️-ways-to-contribute)
  - [👋 Devscode Contribution Ideas](#-devscode-contribution-ideas)
  - [🐛 Report Bugs](#-report-bugs)
  - [✨ Suggest Enhancements](#-suggest-enhancements)
  - [📖 Updating / Improving Documentation](#-updating--improving-documentation)
    - [Running the Documentation Server Locally](#running-the-documentation-server-locally)
      - [Method 1: NPM Script](#method-1-npm-script)
      - [Method 2: VS Code Task](#method-2-vs-code-task)
  - [🧑‍💻 Contributing Code](#-contributing-code)
    - [Environment Setup](#environment-setup)
      - [Pre-requisites](#pre-requisites)
      - [Fork the Devscode Repository with All Branches](#fork-the-devscode-repository)
      - [VS Code](#vs-code)
        - [Debugging](#debugging)
      - [JetBrains](#jetbrains)
    - [Our Git Workflow](#our-git-workflow)
    - [Formatting](#formatting)
    - [Writing Slash Commands](#writing-slash-commands)
    - [Writing Context Providers](#writing-context-providers)
    - [Adding an LLM Provider](#adding-an-llm-provider)
    - [Adding Models](#adding-models)
    - [Adding Pre-indexed Documentation](#adding-pre-indexed-documentation)
  - [📐 Devscode Architecture](#-devscode-architecture)
    - [Devscode VS Code Extension](#devscode-vs-code-extension)
    - [Devscode JetBrains Extension](#devscode-jetbrains-extension)

# ❤️ Ways to Contribute

## 👋 Devscode Contribution Ideas

[This GitHub project board](https://github.com/orgs/khulnasoft/projects/2) is a list of ideas for how you can contribute to Devscode. These aren't the only ways, but are a great starting point if you are new to the project.

## 🐛 Report Bugs

If you find a bug, please [create an issue](https://github.com/khulnasoft/devscode/issues) to report it! A great bug report includes:

- A description of the bug
- Steps to reproduce
- What you expected to happen
- What actually happened
- Screenshots or videos

## ✨ Suggest Enhancements

Devscode is quickly adding features, and we'd love to hear which are the most important to you. The best ways to suggest an enhancement are

- Create an issue

  - First, check whether a similar proposal has already been made
  - If not, [create an issue](https://github.com/khulnasoft/devscode/issues)
  - Please describe the enhancement in as much detail as you can, and why it would be useful

- Join the [Devscode Discord](https://discord.gg/NWtdYexhMs) and tell us about your idea in the `#feedback` channel

## 📖 Updating / Improving Documentation

Devscode is continuously improving, but a feature isn't complete until it is reflected in the documentation! If you see something out-of-date or missing, you can help by clicking "Edit this page" at the bottom of any page on [docs.devscode.dev](https://docs.devscode.dev).

### Running the Documentation Server Locally

You can run the documentation server locally using either of the following methods:

#### Method 1: NPM Script

1. Open your terminal and navigate to the `docs` subdirectory of the project. The `docusaurus.config.js` file you'll see there is a sign you're in the right place.

2. Run the following command to install the necessary dependencies for the documentation server:

   ```bash
   npm install
   ```

3. Run the following command to start the documentation server:

   ```bash
   npm run start
   ```

#### Method 2: VS Code Task

1. Open VS Code in the root directory of the project.

2. Open the VS Code command pallet (`cmd/ctrl+shift+p`) and select `Tasks: Run Task`.

3. Look for the `docs:start` task and select it.

This will start a local server and you can see the documentation rendered in your default browser, typically accessible at `http://localhost:3000`.

## 🧑‍💻 Contributing Code

### Environment Setup

#### Pre-requisites

You should have Node.js version 20.11.0 (LTS) or higher installed. You can get it on [nodejs.org](https://nodejs.org/en/download) or, if you are using NVM (Node Version Manager), you can set the correct version of Node.js for this project by running the following command in the root of the project:

```bash
nvm use
```

#### Fork the Devscode Repository

1. Go to the [Devscode GitHub repository](https://github.com/khulnasoft/devscode) and fork it to your GitHub account.

2. Clone your forked repository to your local machine. Use: `git clone https://github.com/YOUR_USERNAME/devscode.git`

3. Navigate to the cloned directory and make sure you are on the main branch. Create your feature/fix branch from there, like so: `git checkout -b 123-my-feature-branch`

4. Send your pull request to the main branch.

#### VS Code

1. Open the VS Code command pallet (`cmd/ctrl+shift+p`) and select `Tasks: Run Task` and then select `install-all-dependencies`

2. Start debugging:

   1. Switch to Run and Debug view
   2. Select `Launch extension` from drop down
   3. Hit play button
   4. This will start the extension in debug mode and open a new VS Code window with it installed
      1. The new VS Code window with the extension is referred to as the _Host VS Code_
      2. The window you started debugging from is referred to as the _Main VS Code_

3. To package the extension, run `npm run package` in the `extensions/vscode` directory. This will generate `extensions/vscode/build/devscode-{VERSION}.vsix`, which you can install by right-clicking and selecting "Install Extension VSIX".

##### Debugging

**Breakpoints** can be used in both the `core` and `extensions/vscode` folders while debugging, but are not currently supported inside of `gui` code.

**Hot-reloading** is enabled with Vite, so if you make any changes to the `gui`, they should be automatically reflected without rebuilding. In some cases, you may need to refresh the _Host VS Code_ window to see the changes.

Similarly, any changes to `core` or `extensions/vscode` will be automatically included by just reloading the _Host VS Code_ window with cmd/ctrl+shift+p "Reload Window".

#### JetBrains

See the [`CONTRIBUTING.md`](./extensions/intellij/CONTRIBUTING.md) for the JetBrains extension.

### Our Git Workflow

We keep a single permanent branch: `main`. When we are ready to create a "pre-release" version, we create a tag on the `main` branch titled `v0.9.x-vscode`, which automatically triggers the workflow in [preview.yaml](./.github/workflows/preview.yaml), which builds and releases a version of the VS Code extension. When a release has been sufficiently tested, we will create a new release titled `v0.8.x-vscode`, triggering a similar workflow in [main.yaml](./.github/workflows/main.yaml), which will build and release a main release of the VS Code extension. Any hotfixes can be made by creating a feature branch from the tag for the release in question. This workflow is well explained by http://releaseflow.org.

### Formatting

Devscode uses [Prettier](https://marketplace.visualstudio.com/items?itemName=esbenp.prettier-vscode) to format JavaScript/TypeScript. Please install the Prettier extension in VS Code and enable "Format on Save" in your settings.

### Writing Slash Commands

The slash command interface, defined in [core/index.d.ts](./core/index.d.ts), requires you to define a `name` (the text that will be typed to invoke the command), a `description` (the text that will be shown in the slash command menu), and a `run` function that will be called when the command is invoked. The `run` function is an async generator that yields the content to be displayed in the chat. The `run` function is passed a `DevscodeSDK` object that can be used to interact with the IDE, call the LLM, and see the chat history, among a few other utilities.

```ts
export interface SlashCommand {
  name: string;
  description: string;
  params?: { [key: string]: any };
  run: (sdk: DevscodeSDK) => AsyncGenerator<string | undefined>;
}
```

There are many example of slash commands in [core/commands/slash](./core/commands/slash) that we recommend borrowing from. Once you've created your new `SlashCommand` in this folder, also be sure to complete the following:

- Add your command to the array in [core/commands/slash/index.ts](./core/commands/slash/index.ts)
- Add your command to the list in [`config_schema.json`](./extensions/vscode/config_schema.json). This makes sure that Intellisense shows users what commands are available for your provider when they are editing `config.json`. If there are any parameters that your command accepts, you should also follow existing examples in adding them to the JSON Schema.

### Writing Context Providers

A `ContextProvider` is a Devscode plugin that lets type '@' to quickly select documents as context for the language model. The `IContextProvider` interface is defined in [`core/index.d.ts`](./core/index.d.ts), but all built-in context providers extend [`BaseContextProvider`](./core/context/index.ts).

Before defining your context provider, determine which "type" you want to create. The `"query"` type will show a small text input when selected, giving the user the chance to enter something like a Google search query for the [`GoogleContextProvider`](./core/context/providers/GoogleContextProvider.ts). The `"submenu"` type will open up a submenu of items that can be searched through and selected. Examples are the [`GitHubIssuesContextProvider`](./core/context/providers/GitHubIssuesContextProvider.ts) and the [`DocsContextProvider`](./core/context/providers/DocsContextProvider.ts). The `"normal"` type will just immediately add the context item. Examples include the [`DiffContextProvider`](./core/context/providers/DiffContextProvider.ts) and the [`OpenFilesContextProvider`](./core/context/providers/OpenFilesContextProvider.ts).

After you've written your context provider, make sure to complete the following:

- Add it to the array of context providers in [core/context/providers/index.ts](./core/context/providers/index.ts)
- Add it to the `ContextProviderName` type in [core/index.d.ts](./core/index.d.ts)
- Add it to the list in [`config_schema.json`](./extensions/vscode/config_schema.json). If there are any parameters that your context provider accepts, you should also follow existing examples in adding them to the JSON Schema.

### Adding an LLM Provider

Devscode has support for more than a dozen different LLM "providers", making it easy to use models running on OpenAI, Ollama, Together, LM Studio, Msty, and more. You can find all of the existing providers [here](https://github.com/khulnasoft/devscode/tree/main/core/llm/llms), and if you see one missing, you can add it with the following steps:

1. Create a new file in the `core/llm/llms` directory. The name of the file should be the name of the provider, and it should export a class that extends `BaseLLM`. This class should contain the following minimal implementation. We recommend viewing pre-existing providers for more details. The [LlamaCpp Provider](./core/llm/llms/LlamaCpp.ts) is a good simple example.

- `providerName` - the identifier for your provider
- At least one of `_streamComplete` or `_streamChat` - This is the function that makes the request to the API and returns the streamed response. You only need to implement one because Devscode can automatically convert between "chat" and "raw completion".

2. Add your provider to the `LLMs` array in [core/llm/llms/index.ts](./core/llm/llms/index.ts).
3. If your provider supports images, add it to the `PROVIDER_SUPPORTS_IMAGES` array in [core/llm/autodetect.ts](./core/llm/autodetect.ts).
4. Add the necessary JSON Schema types to [`config_schema.json`](./extensions/vscode/config_schema.json). This makes sure that Intellisense shows users what options are available for your provider when they are editing `config.json`.
5. Add a documentation page for your provider in [`docs/docs/customize/model-providers`](./docs/docs/customize/model-providers). This should show an example of configuring your provider in `config.json` and explain what options are available.

### Adding Models

While any model that works with a supported provider can be used with Devscode, we keep a list of recommended models that can be automatically configured from the UI or `config.json`. The following files should be updated when adding a model:

- [config_schema.json](./extensions/vscode/config_schema.json) - This is the JSON Schema definition that is used to validate `config.json`. You'll notice a number of rules defined in "definitions.ModelDescription.allOf". Here is where you write rules that can specify something like "for the provider 'anthropic', only models 'claude-2' and 'claude-instant-1' are allowed. Look through all of these rules and make sure that your model is included for providers that support it.
- [AddNewModel page](./gui/src/pages/AddNewModel) - This directory defines which model options are shown in the side bar model selection UI. To add a new model:
  1. Add a `ModelPackage` entry for the model into [configs/models.ts](./gui/src/pages/AddNewModel/configs/models.ts), following the lead of the many examples near the top of the file
  2. Add the model within its provider's array to [AddNewModel.tsx](./gui/src/pages/AddNewModel/AddNewModel.tsx) (add provider if needed)
- [index.d.ts](./core/index.d.ts) - This file defines the TypeScript types used throughout Devscode. You'll find a `ModelName` type. Be sure to add the name of your model to this.
- LLM Providers: Since many providers use their own custom strings to identify models, you'll have to add the translation from Devscode's model name (the one you added to `index.d.ts`) and the model string for each of these providers: [Ollama](./core/llm/llms/Ollama.ts), [Together](./core/llm/llms/Together.ts), and [Replicate](./core/llm/llms/Replicate.ts). You can find their full model lists here: [Ollama](https://ollama.ai/library), [Together](https://docs.together.ai/docs/inference-models), [Replicate](https://replicate.com/collections/streaming-language-models).
- [Prompt Templates](./core/llm/index.ts) - In this file you'll find the `autodetectTemplateType` function. Make sure that for the model name you just added, this function returns the correct template type. This is assuming that the chat template for that model is already built in Devscode. If not, you will have to add the template type and corresponding edit and chat templates.

### Adding Pre-indexed Documentation

Devscode's @docs context provider lets you easily reference entire documentation sites and then uses embeddings to add the most relevant pages to context. To make the experience as smooth as possible, we pre-index many of the most popular documentation sites. If you'd like to add new documentation to this list, just add an object to the list in [preIndexedDocs.ts](./core/indexing/docs/preIndexedDocs.ts). `startUrl` is where the crawler will start and `rootUrl` will filter out any pages not on that site and under the path of `rootUrl`.

## 📐 Devscode Architecture

Devscode consists of 2 parts that are split so that it can be extended to work in other IDEs as easily as possible:

1. **Devscode GUI** - The Devscode GUI is a React application that gives the user control over Devscode. It displays the current chat history, allows the user to ask questions, invoke slash commands, and use context providers. The GUI also handles most state and holds as much of the logic as possible so that it can be reused between IDEs.

2. **Devscode Extension** - The Devscode Extension is a plugin for the IDE which implements the [IDE Interface](./core/index.d.ts#L229). This allows the GUI to request information from or actions to be taken within the IDE. This same interface is used regardless of IDE. The first Devscode extensions we have built are for VS Code and JetBrains, but we plan to build clients for other IDEs in the future. The IDE Client must 1. implement IDE Interface, as is done [here](./extensions/vscode/src/ideProtocol.ts) for VS Code and 2. display the Devscode GUI in a sidebar, like [here](./extensions/vscode/src/DevscodeGUIWebviewViewProvider.ts).

### Devscode VS Code Extension

The starting point for the VS Code extension is [activate.ts](./extensions/vscode/src/activation/activate.ts). The `activateExtension` function here will register all commands and load the Devscode GUI in the sidebar of the IDE as a webview.

### Devscode JetBrains Extension

The JetBrains extension is currently in alpha testing. Please reach out on [Discord](https://discord.gg/vapESyrFmJ) if you are interested in contributing to its development.
