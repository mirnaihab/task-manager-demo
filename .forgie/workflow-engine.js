// Loads the workflow engine with project options. Forgie 1.0.5 accepts only plugin
// paths in forgie.jsonc, not [path, options] pairs, so the options are set here.
import { WorkflowEnginePlugin } from "./node_modules/forgie-workflow-engine/dist/plugin.js"

// The model gateway sends nothing while it writes a large file. The default stall
// limit (240000 ms) stops healthy calls. 900000 ms = 15 minutes.
export const WorkflowEngine = (input) => WorkflowEnginePlugin(input, { modelStallMs: 900000 })
