---
title: Overview
description: Introduction to customizing Devscode
keywords: [customize, configure, config]
---

Devscode can be deeply customized. This is primarily accomplished by editing a local file located at `~/.devscode/config.json` (MacOS / Linux) or `%USERPROFILE%\.devscode\config.json` (Windows). `config.json` is created the first time you use Devscode.

## Getting started

To open `config.json`, click the "gear" icon in the bottom right corner of the Devscode Chat sidebar. When editing this file, you can see the available options suggested as you type, or you can check the [full reference](./deep-dives/configuration.md).

When you save `config.json`, Devscode will automatically refresh to take into account your changes.

## Per-workspace configuration

If you'd like to scope certain settings to a particular workspace, you can add a `.devscoderc.json` to the root of your project. It has the same [definition](./deep-dives/configuration.md) as `config.json`, and will automatically be applied on top of the local config.json.

## Programmatic configuration

`config.json` can handle the vast majority of necessary configuration, so we recommend using it whenever possible. However, if you need to programmatically configure Devscode, you can use `config.ts`, which is located at `~/.devscode/config.ts` (MacOS / Linux) or `%USERPROFILE%\.devscode\config.ts` (Windows).

For examples of how to use `config.ts`, see [writing custom slash commands](./tutorials/build-your-own-slash-command.md#custom-slash-commands) or [writing custom context providers](./tutorials/build-your-own-context-provider.md).
