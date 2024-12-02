---
title: Free Trial
slug: ../free-trial
---

The `"free-trial"` provider lets new users quickly try out the best experience in Devscode using our API keys through a secure proxy server. To prevent abuse, we will ask you to sign in with GitHub, which you can read more about below.

While the Devscode extension is always free to use, we cannot support infinite free LLM usage for all of our users. You will eventually need to either:

1. Select an open-source model to use for free [locally](../top-level/ollama.md), or
2. Add your own API key for [Anthropic](../top-level/anthropic.md) or [another LLM provider](/customize/model-providers)

## FAQ about the trial

Devscode asks free trial users to sign in so that we can prevent abuse of our API endpoints. If you are not using the free trial, we will not ask you to sign in.

### How do I stop Devscode from asking me to sign in?

Remove all models from the "models" array or "tabAutocompleteModel" with `"provider": "free-trial"`, and we will never request sign in.

### What information is collected?

Devscode uses your GitHub username and no other information, for the sole purpose of limiting requests.

### What happens if I don't sign in?

If you don't sign in, you can still use every feature of Devscode, you will just need to provide your own LLM either with an API key or by running a local model.

### How is telemetry related to sign in?

It is not. We do not link your GitHub username to telemetry data.
