---
title: Using Llama 3.1 with Devscode
description: How to use Llama 3.1 with Devscode
keywords: [llama, meta, togetherai, ollama, replicate]
---

Devscode makes it easy to code with the latest open-source models, including the entire Llama 3.1 family of models. Llama 3.2 models are also supported but not recommended for chat, because they are specifically designed to be small or multi-modal.

If you haven't already installed Devscode, you can do that [here for VS Code](https://marketplace.visualstudio.com/items?itemName=Devscode.devscode) or [here for JetBrains](https://plugins.jetbrains.com/plugin/22707-devscode). For more general information on customizing Devscode, read [our customization docs](../overview.md).

Below we share some of the easiest ways to get up and running, depending on your use-case.

## Ollama

Ollama is the fastest way to get up and running with local language models. We recommend trying Llama 3.1 8b, which is impressive for its size and will perform well on most hardware.

1. Download Ollama [here](https://ollama.ai/) (it should walk you through the rest of these steps)
2. Open a terminal and run `ollama run llama3.1:8b`
3. Change your Devscode config file like this:

```json title="config.json"
{
  "models": [
    {
      "title": "Llama 3.1 8b",
      "provider": "ollama",
      "model": "llama3.1-8b"
    }
  ]
}
```

## Groq

Groq provides the fastest available inference for open-source language models, including the entire Llama 3.1 family.

1. Obtain an API key [here](https://console.groq.com/keys)
2. Update your Devscode config file like this:

```json title="config.json"
{
  "models": [
    {
      "title": "Llama 3.1 405b",
      "provider": "groq",
      "model": "llama3.1-405b",
      "apiKey": "<API_KEY>"
    }
  ]
}
```

## Together AI

Together AI provides fast and reliable inference of open-source models. You'll be able to run the 405b model with good speed.

1. Create an account [here](https://api.together.xyz/signup)
2. Copy your API key that appears on the welcome screen
3. Update your Devscode config file like this:

```json title="config.json"
{
  "models": [
    {
      "title": "Llama 3.1 405b",
      "provider": "together",
      "model": "llama3.1-405b",
      "apiKey": "<API_KEY>"
    }
  ]
}
```

## Replicate

Replicate makes it easy to host and run open-source AI with an API.

1. Get your Replicate API key [here](https://replicate.ai/)
2. Change your Devscode config file like this:

```json title="config.json"
{
  "models": [
    {
      "title": "Llama 3.1 405b",
      "provider": "replicate",
      "model": "llama3.1-405b",
      "apiKey": "<API_KEY>"
    }
  ]
}
```

## SambaNova

SambaNova Cloud provides world record Llama3.1 70B/405B serving.

1. Create an account [here](https://cloud.sambanova.ai/)
2. Copy your API key
3. Update your Devscode config file like this:

```json title="~/.devscode/config.json"
{
  "models": [
    {
      "title": "SambaNova Llama 3.1 405B",
      "provider": "sambanova",
      "model": "llama3.1-405b",
      "apiKey": "YOUR_API_KEY"
    }
  ]
}
```

## Cerebras Inference

Cerebras Inference uses specialized silicon to provides fast inference for the Llama3.1 8B/70B.

1. Create an account in the portal [here](https://cloud.cerebras.ai/).
2. Create and copy the API key for use in Devscode.
3. Update your Devscode config file:

```json title="config.json"
{
  "models": [
    {
      "title": "Cerebras Llama 3.1 70B",
      "provider": "cerebras",
      "model": "llama3.1-70b",
      "apiKey": "YOUR_API_KEY"
    }
  ]
}
```
