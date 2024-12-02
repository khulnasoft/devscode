---
title: Embeddings model
description: Embeddings model
keywords: [embedding]
sidebar_position: 3
---

An "embeddings model" is trained to convert a piece of text into a vector, which can later be rapidly compared to other vectors to determine similarity between the pieces of text. Embeddings models are typically much smaller than LLMs, and will be extremely fast and cheap in comparison.

In Devscode, embeddings are generated during indexing and then used by [@Codebase](../deep-dives/codebase.md) to perform similarity search over your codebase.

:::info[Default model (VS Code only)]
`transformers.js` is used as a default embeddings model in VS Code. In JetBrains, there currently is no default.
:::

:::info[Free Trial]
The free trial supports embeddings (provider: `free-trial`)
:::

## Recommended embedding models

If you have the ability to use any model, we recommend `voyage-code-2`, which is listed below along with the rest of the options for embeddings models.

If you want to generate embeddings locally, we recommend using `nomic-embed-text` with [Ollama](../model-providers/top-level/ollama.md#embeddings-model).

### Voyage AI

After obtaining an API key from [here](https://www.voyageai.com/), you can configure like this:

```json title="config.json"
{
  "embeddingsProvider": {
    "provider": "voyage",
    "model": "voyage-code-2",
    "apiKey": "<VOYAGE_API_KEY>"
  }
}
```

### Ollama

See [here](../model-providers/top-level/ollama.md#embeddings-model) for instructions on how to use Ollama for embeddings.

### Transformers.js (currently VS Code only)

[Transformers.js](https://huggingface.co/docs/transformers.js/index) is a JavaScript port of the popular [Transformers](https://huggingface.co/transformers/) library. It allows embeddings to be calculated entirely locally. The model used is `all-MiniLM-L6-v2`, which is shipped alongside the Devscode extension.

```json title="config.json"
{
  "embeddingsProvider": {
    "provider": "transformers.js"
  }
}
```

### Text Embeddings Inference

[Hugging Face Text Embeddings Inference](https://huggingface.co/docs/text-embeddings-inference/en/index) enables you to host your own embeddings endpoint. You can configure embeddings to use your endpoint as follows:

```json title="config.json"
{
  "embeddingsProvider": {
    "provider": "huggingface-tei",
    "apiBase": "http://localhost:8080"
  }
}
```

### OpenAI

See [here](../model-providers/top-level/openai.md#embeddings-model) for instructions on how to use OpenAI for embeddings.

### Cohere

See [here](../model-providers/more/cohere.md#embeddings-model) for instructions on how to use Cohere for embeddings.

### Gemini

See [here](../model-providers/top-level/gemini.md#embeddings-model) for instructions on how to use Gemini for embeddings.

### Vertex

See [here](../model-providers/top-level/vertexai.md#embeddings-model) for instructions on how to use Vertex for embeddings.

### Mistral

See [here](../model-providers/top-level/mistral.md#embeddings-model) for instructions on how to use Mistral for embeddings.

### NVIDIA

See [here](../model-providers/more/nvidia.md#embeddings-model) for instructions on how to use NVIDIA for embeddings.

### Bedrock

See [here](../model-providers/top-level/bedrock.md#embeddings-model) for instructions on how to use Bedrock for embeddings.

### WatsonX

See [here](../model-providers/more/watsonx.md#embeddings-model) for instructions on how to use WatsonX for embeddings.

### LMStudio

See [here](../model-providers/more/lmstudio.md#embeddings-model) for instructions on how to use LMStudio for embeddings.