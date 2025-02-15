# Binary name
BINARY_NAME=start-wrapper

# Build directory
BUILD_DIR=build

# Version
VERSION=$(shell git describe --tags --always --dirty 2>/dev/null || echo "dev")

# Build flags
LD_FLAGS=-ldflags "-X main.Version=$(VERSION)"

# Disable CGO for cross-compilation
CGO_ENABLED=0
export CGO_ENABLED

# Platform-specific architectures
WINDOWS_ARCH=amd64 386 arm arm64
LINUX_ARCH=amd64 386 arm arm64 ppc64 ppc64le mips mips64 mipsle mips64le riscv64 s390x
DARWIN_ARCH=amd64 arm64
FREEBSD_ARCH=amd64 386 arm arm64
OPENBSD_ARCH=amd64 386 arm arm64
NETBSD_ARCH=amd64 386 arm arm64
DRAGONFLY_ARCH=amd64
SOLARIS_ARCH=amd64
AIX_ARCH=ppc64
ILLUMOS_ARCH=amd64
PLAN9_ARCH=amd64 386 arm

# Default task
.PHONY: all
all: clean build-all

# Clean build directory
.PHONY: clean
clean:
	rm -rf $(BUILD_DIR)

# Create build directory
.PHONY: init
init:
	mkdir -p $(BUILD_DIR)

# Build for all platforms
.PHONY: build-all
build-all: init windows linux darwin bsd special

# Windows builds
.PHONY: windows
windows:
	@echo "Building for Windows..."
	@for arch in $(WINDOWS_ARCH); do \
		echo "Building for windows/$$arch..."; \
		GOOS=windows GOARCH=$$arch go build $(LD_FLAGS) -o $(BUILD_DIR)/$(BINARY_NAME)-windows-$$arch.exe .; \
	done

# Linux builds
.PHONY: linux
linux:
	@echo "Building for Linux..."
	@for arch in $(LINUX_ARCH); do \
		echo "Building for linux/$$arch..."; \
		GOOS=linux GOARCH=$$arch go build $(LD_FLAGS) -o $(BUILD_DIR)/$(BINARY_NAME)-linux-$$arch .; \
	done

# Darwin (macOS) builds
.PHONY: darwin
darwin:
	@echo "Building for macOS..."
	@for arch in $(DARWIN_ARCH); do \
		echo "Building for darwin/$$arch..."; \
		GOOS=darwin GOARCH=$$arch go build $(LD_FLAGS) -o $(BUILD_DIR)/$(BINARY_NAME)-darwin-$$arch .; \
	done

# BSD builds
.PHONY: bsd
bsd:
	@echo "Building for FreeBSD..."
	@for arch in $(FREEBSD_ARCH); do \
		echo "Building for freebsd/$$arch..."; \
		GOOS=freebsd GOARCH=$$arch go build $(LD_FLAGS) -o $(BUILD_DIR)/$(BINARY_NAME)-freebsd-$$arch .; \
	done
	@echo "Building for OpenBSD..."
	@for arch in $(OPENBSD_ARCH); do \
		echo "Building for openbsd/$$arch..."; \
		GOOS=openbsd GOARCH=$$arch go build $(LD_FLAGS) -o $(BUILD_DIR)/$(BINARY_NAME)-openbsd-$$arch .; \
	done
	@echo "Building for NetBSD..."
	@for arch in $(NETBSD_ARCH); do \
		echo "Building for netbsd/$$arch..."; \
		GOOS=netbsd GOARCH=$$arch go build $(LD_FLAGS) -o $(BUILD_DIR)/$(BINARY_NAME)-netbsd-$$arch .; \
	done
	@echo "Building for DragonFly BSD..."
	@for arch in $(DRAGONFLY_ARCH); do \
		echo "Building for dragonfly/$$arch..."; \
		GOOS=dragonfly GOARCH=$$arch go build $(LD_FLAGS) -o $(BUILD_DIR)/$(BINARY_NAME)-dragonfly-$$arch .; \
	done

# Special platform builds
.PHONY: special
special:
	@echo "Building for special platforms..."
	@echo "Building for Solaris..."
	@for arch in $(SOLARIS_ARCH); do \
		GOOS=solaris GOARCH=$$arch go build $(LD_FLAGS) -o $(BUILD_DIR)/$(BINARY_NAME)-solaris-$$arch; \
	done
	@echo "Building for AIX..."
	@for arch in $(AIX_ARCH); do \
		GOOS=aix GOARCH=$$arch go build $(LD_FLAGS) -o $(BUILD_DIR)/$(BINARY_NAME)-aix-$$arch; \
	done
	@echo "Building for IllumOS..."
	@for arch in $(ILLUMOS_ARCH); do \
		GOOS=illumos GOARCH=$$arch go build $(LD_FLAGS) -o $(BUILD_DIR)/$(BINARY_NAME)-illumos-$$arch; \
	done
	@echo "Building for Plan9..."
	@for arch in $(PLAN9_ARCH); do \
		GOOS=plan9 GOARCH=$$arch go build $(LD_FLAGS) -o $(BUILD_DIR)/$(BINARY_NAME)-plan9-$$arch; \
	done

# Create release archives
.PHONY: release
release: build-all
	@echo "Creating release archives..."
	@cd $(BUILD_DIR) && \
	for file in $(BINARY_NAME)-*; do \
		if [[ $$file == *.exe ]]; then \
			zip "$$file.zip" "$$file"; \
		else \
			tar czf "$$file.tar.gz" "$$file"; \
		fi \
	done

# Build for current platform (for development)
.PHONY: build
build:
	go build $(LD_FLAGS) -o $(BUILD_DIR)/$(BINARY_NAME)

# Run tests
.PHONY: test
test:
	go test -v ./...

# Show available targets
.PHONY: help
help:
	@echo "Available targets:"
	@echo "  all      - Clean and build for all platforms"
	@echo "  clean    - Remove build directory"
	@echo "  build    - Build for current platform"
	@echo "  release  - Create release archives"
	@echo "  test     - Run tests"
	@echo ""
	@echo "Platform-specific builds:"
	@echo "  windows  - Build Windows binaries"
	@echo "  linux    - Build Linux binaries"
	@echo "  darwin   - Build macOS binaries"
	@echo "  bsd      - Build BSD binaries"
	@echo "  special  - Build for other platforms"
