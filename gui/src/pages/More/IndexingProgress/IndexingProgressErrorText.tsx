import { IndexingProgressUpdate } from "core";
import TransformersJsEmbeddingsProvider from "core/indexing/embeddings/TransformersJsEmbeddingsProvider";
import { isJetBrains } from "../../../util";
import { XCircleIcon } from "@heroicons/react/24/outline";
import { useAppSelector } from "../../../redux/hooks";

export interface IndexingProgressErrorTextProps {
  update: IndexingProgressUpdate;
}

function IndexingProgressErrorText({ update }: IndexingProgressErrorTextProps) {
  const embeddingsProvider = useAppSelector(
    (state) => state.config.config.embeddingsProvider,
  );

  const showJbError =
    (isJetBrains() &&
      embeddingsProvider === TransformersJsEmbeddingsProvider.model) ||
    true;

  if (showJbError) {
    return (
      <div className="flex items-center gap-2 italic">
        <XCircleIcon className="h-4 w-4 min-w-[10%]" />
        <span className="leading-relaxed">
          The <code>transformers.js</code> embeddingsProvider is currently
          unsupported in JetBrains. To enable codebase indexing, you can use any
          of the other providers described in the docs:{" "}
          <a
            href="https://docs.devscode.dev/walkthroughs/codebase-embeddings#embeddings-providers"
            target="_blank"
            className="cursor-pointer text-inherit underline hover:text-inherit"
          >
            https://docs.devscode.dev/walkthroughs/codebase-embeddings#embeddings-providers
          </a>
        </span>
      </div>
    );
  }

  return (
    <div className="flex items-center gap-2 italic">
      <XCircleIcon className="h-4 w-4" />
      <span className="leading-relaxed">{update.desc}</span>
    </div>
  );
}

export default IndexingProgressErrorText;
