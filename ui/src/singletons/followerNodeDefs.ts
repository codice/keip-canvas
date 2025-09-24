import { CustomEdge } from "../api/flow"
import { ConnectionType, EipId } from "../api/generated/eipFlow"
import { lookupEipComponent } from "./eipDefinitions"
import { getEipId } from "./store/storeViews"

export interface FollowerNodeDescriptor {
  eipId: EipId
  generateLabel: (leaderLabel: string) => string

  hiddenEdge?: (leaderId: string, followerId: string) => Partial<CustomEdge>
  overrides?: {
    connectionType: ConnectionType
  }
}

// Note: currently we assume at most one follower node is allowed per leader,
// this constraint may be relaxed in the future if a use case requiring multiple followers appears.
export const describeFollower = (
  leaderId: EipId
): FollowerNodeDescriptor | null => {
  const leaderComponent = lookupEipComponent(leaderId)
  if (!leaderComponent) {
    return null
  }

  if (leaderComponent.connectionType === "inbound_request_reply") {
    // For greater separation of concerns, consider returning a node generator function instead.
    return {
      eipId: { namespace: "integration", name: "channel" },
      generateLabel: (leaderLabel) => `${leaderLabel}-reply-channel`,
      hiddenEdge: (leaderId, followerId) => ({
        source: followerId,
        target: leaderId,
      }),
      overrides: {
        connectionType: "sink",
      },
    }
  }

  return null
}

export const describeFollowerFromId = (leaderId: string) => {
  const eipId = getEipId(leaderId)
  return eipId ? describeFollower(eipId) : null
}
