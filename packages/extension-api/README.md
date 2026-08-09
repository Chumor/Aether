# @baimoqilin/aether-extension-api

TypeScript declarations for Aether Script Extensions.

```bash
npm install --save-dev @baimoqilin/aether-extension-api
```

```ts
import { defineAetherExtension, ui } from "@baimoqilin/aether-extension-api";

export const activateAether = defineAetherExtension((aether) => {
  aether.registerSurface("chat.composer.top", {
    render: () => ui.text("Hello from Aether"),
  });
});
```

Extensions can register native-looking settings without building a separate
screen. Values are stored per extension and restored across reloads:

```ts
aether.registerSettings({
  id: "preferences",
  title: "Preferences",
  sections: [{
    title: "General",
    settings: [
      { id: "enabled", label: "Enabled", type: "toggle", default: true },
      { id: "endpoint", label: "Endpoint", type: "text", placeholder: "https://..." },
      { id: "mode", label: "Mode", type: "select", options: [
        { value: "fast", label: "Fast" },
        { value: "quality", label: "Quality" },
      ] },
    ],
  }],
});
```

The chat composer plus menu and transcript support extension-owned entries:

```ts
aether.registerComposerMenuItem({
  id: "summarize",
  title: "Summarize thread",
  icon: "auto",
  action: "summarize",
});
aether.registerMessageType({
  type: "summary",
  title: "Summary",
  render: ({ message }) => ui.card([
    ui.text(String(message.title ?? "Summary")),
    ui.text(String(message.body ?? ""), { color: "muted" }),
  ]),
});
aether.registerAction("show-summary", () =>
  aether.messages.append("summary", { title: "Done", body: "Thread summarized." }));
```

This package intentionally contains declarations only. Aether injects the
runtime implementation when it loads an Extension.

The npm package major version matches `aether.apiVersion`. Package `2.x`
describes Script API version 2.
