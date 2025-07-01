# Keip Canvas Assistant

This repository contains the necessary files to build and run the Keip Canvas Assistant using Ollama. The assistant helps with creating flow diagrams in the Keip Canvas UI.

## Overview

The Keip Canvas UI provides experimental support for generative AI models to aid in creating flow diagrams. The Canvas can integrate with LLMs running through [Ollama](https://github.com/ollama/ollama).

The `keip-assistant:32b` model is designed to help users create and modify flow diagrams through natural language interactions.

### Prerequisites
- [Ollama](https://github.com/ollama/ollama) installed on your system (`v0.9+`)
- The Modelfile and Makefile from this repository

### Install Ollama
Install [Ollama](https://github.com/ollama/ollama?tab=readme-ov-file#ollama) following the instructions for your platform.
- To allow the Canvas UI to access the local Ollama server, add the following web origin:
  ```shell
  OLLAMA_ORIGINS="https://codice.org"
  ```
  [See here](https://github.com/ollama/ollama/blob/main/docs/faq.md#how-do-i-configure-ollama-server) for
  details on configuring the Ollama server.

If you run into issues, check out
Ollama's [troubleshooting suggestions](https://github.com/ollama/ollama/blob/main/docs/troubleshooting.md).


### Build the Assistant Model
Navigate to the directory containing the Modelfile and Makefile, then run:

```bash
make build
```

This command creates a model named `keip-assistant:32b` using the specifications in the Modelfile.

### Verify the Model
To check that the model was built successfully:

```bash
make status
```

### Navigate to Keip Canvas
In a web browser, navigate to the [Canvas](https://codice.org/keip-canvas/).
The `KeipAssistant` button should be enabled in the bottom toolbar. Click it to open a chat panel.

### Example Prompts
Try these example prompts with the assistant:

```
Return a flow that listens to a JMS topic and forwards the message out to a Kafka topic
```

Followed by:

```
Add a correlation id header to the message before sending to Kafka
```

Note: The existing diagram is included in the chat context, allowing the assistant to understand and modify the current state of your flow diagram.

## Makefile Commands

The included Makefile provides several useful commands:

- `make build` - Build the assistant model
- `make run` - Run the assistant in interactive mode
- `make status` - Show information about the model
- `make clean` - Remove the model
- `make config-ollama` - Configure the Ollama URL (local or remote)
- `make help` - Display help information

## Customization

To customize the assistant, modify the Modelfile before building. The Modelfile contains the model's configuration, including its base model, system prompts, and other parameters.

## Troubleshooting

- If you encounter build errors, check that Ollama is properly installed and running
- For connection issues with remote Ollama, verify network connectivity and firewall settings
- Ensure you have sufficient disk space for the model
- Verify that your system meets the minimum requirements for running 32B parameter models
- Check Ollama's [troubleshooting suggestions](https://github.com/ollama/ollama/blob/main/docs/troubleshooting.md) for common issues

## License

Please refer to the LICENSE file for information about using and distributing this model.

