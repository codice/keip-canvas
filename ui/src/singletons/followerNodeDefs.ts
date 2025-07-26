import { ConnectionType, EipId } from "../api/generated/eipFlow"
import { lookupEipComponent } from "./eipDefinitions"

interface FollowerNodeDescriptor {
  eipId: EipId
  generateLabel: (leaderLabel: string) => string
  overrides?: {
    connectionType: ConnectionType
  }
}

export const describeFollowers = (
  leaderId: EipId
): FollowerNodeDescriptor[] => {
  const leaderComponent = lookupEipComponent(leaderId)
  if (!leaderComponent) {
    return []
  }

  if (leaderComponent.connectionType === "inbound_request_reply") {
    // For greater separation of concerns, consider returning a node generator function instead.
    return [
      {
        eipId: { namespace: "integration", name: "channel" },
        generateLabel: (leaderLabel) => `${leaderLabel}-reply-channel`,
        overrides: {
          connectionType: "sink",
        },
      },
    ]
  }

  return []
}
