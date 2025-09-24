import { nanoid } from "nanoid/non-secure"

export const generateNodeId = () => nanoid(10)

export const generateChildId = () => nanoid(11)
