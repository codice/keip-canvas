# Keip Canvas

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![GitHub Pages Pipeline](https://github.com/codice/keip-canvas/actions/workflows/deploy-canvas-webapp.yaml/badge.svg)](https://github.com/codice/keip-canvas/actions/workflows/deploy-canvas-webapp.yaml)

Intuitive drag and drop UI for creating deployable integration flow diagrams.

Powered by [ReactFlow](https://reactflow.dev/) and [Spring Integration](https://spring.io/projects/spring-integration)


<img src="docs/img/canvas-demo.gif" alt="KeipCanvasDemo" width=800>

## Why Keip Canvas?

We wanted a tool that would allow us to combine the flexibility of Spring Integration's XML configuration with the
understandability of a diagram. Consequently, Keip Canvas is designed to let you:

- Rapidly create data flows with
  full [Enterprise Integration Patterns](https://www.enterpriseintegrationpatterns.com/patterns/messaging/) support
- Easily deploy diagrams to a Kubernetes cluster powered by [keip](https://github.com/codice/keip)
- Focus on a flow's business logic rather than XML boilerplate
- Share and collaborate on visual models rather than verbose XML

## Usage

Keip Canvas is hosted at https://codice.org/keip-canvas/

## Development

To get started with developing the Canvas UI, see the [dev docs](./ui/DEVELOPER.md).

## Architecture

For more details on how Keip Canvas is implemented, see the [architecture documentation](docs/ARCHITECTURE.md).

## Contributing

We welcome contributions! Please see our [contributing guidelines](docs/CONTRIBUTING.md) for more details.

## Support

For assistance or to report issues, please open an issue in the GitHub repository.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.