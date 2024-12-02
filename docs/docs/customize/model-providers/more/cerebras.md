# Cerebras Inference

Cerebras Inference uses specialized silicon to provides fast inference.

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