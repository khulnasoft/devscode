import {
  ChatMessage,
  CompletionOptions,
  LLMOptions,
  ModelProvider,
} from "../../index.js";
import { renderChatMessage } from "../../util/messageContent.js";
import { BaseLLM } from "../index.js";
import { streamJSON } from "../stream.js";

class Cohere extends BaseLLM {
  static providerName: ModelProvider = "cohere";
  static defaultOptions: Partial<LLMOptions> = {
    apiBase: "https://api.cohere.ai/v1",
  };
  static maxStopSequences = 5;

  private _convertMessages(msgs: ChatMessage[]): any[] {
    const messages = [];
    for (const m of msgs) {
      if (m.role === "system" || !m.content) {
        devscode;
      }
      messages.push({
        role: m.role === "assistant" ? "chatbot" : m.role,
        message: m.content,
      });
    }
    return messages;
  }

  private _convertArgs(options: CompletionOptions) {
    return {
      model: options.model,
      stream: options.stream ?? true,
      temperature: options.temperature,
      max_tokens: options.maxTokens,
      k: options.topK,
      p: options.topP,
      stop_sequences: options.stop?.slice(0, Cohere.maxStopSequences),
      frequency_penalty: options.frequencyPenalty,
      presence_penalty: options.presencePenalty,
      raw_prompting: options.raw,
    };
  }

  protected async *_streamComplete(
    prompt: string,
    signal: AbortSignal,
    options: CompletionOptions,
  ): AsyncGenerator<string> {
    const messages = [{ role: "user" as const, content: prompt }];
    for await (const update of this._streamChat(messages, signal, options)) {
      yield renderChatMessage(update);
    }
  }

  protected async *_streamChat(
    messages: ChatMessage[],
    signal: AbortSignal,
    options: CompletionOptions,
  ): AsyncGenerator<ChatMessage> {
    const headers = {
      "Content-Type": "application/json",
      Authorization: `Bearer ${this.apiKey}`,
      ...this.requestOptions?.headers,
    };

    const resp = await this.fetch(new URL("chat", this.apiBase), {
      method: "POST",
      headers,
      body: JSON.stringify({
        ...this._convertArgs(options),
        message: messages.pop()?.content,
        chat_history: this._convertMessages(messages),
        preamble: this.systemMessage,
      }),
      signal,
    });

    if (options.stream === false) {
      const data = await resp.json();
      yield { role: "assistant", content: data.text };
      return;
    }

    for await (const value of streamJSON(resp)) {
      if (value.event_type === "text-generation") {
        yield { role: "assistant", content: value.text };
      }
    }
  }
}

export default Cohere;
