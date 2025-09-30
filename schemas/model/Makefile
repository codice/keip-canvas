# Usage:
#   make update-schema-version
#
# This will replace all occurrences of
#   https://github.com/codice/keip-canvas/schemas/v<old>/...
# with
#   https://github.com/codice/keip-canvas/schemas/v0.4.2/...

VERSION_FILE := schema_bundle_version.txt
VERSION := $(strip $(file < $(VERSION_FILE)))

update-schema-version:
	@if ! echo "$(VERSION)" | grep -Eq '^[0-9]+\.[0-9]+\.[0-9]+$$'; then \
		echo "ERROR: Invalid version '$(VERSION)'. Expected format: X.Y.Z"; \
		exit 1; \
	fi

	@echo "Updating schema bundle version to v$(VERSION)..."
	@find json -type f -name '*.json' -print0 | \
		xargs -0 sed -i -E "s|(https://github.com/codice/keip-canvas/schemas/)v[0-9]+\.[0-9]+\.[0-9]+|\1v$(VERSION)|g"
	@echo "Done."
