# Base images are pinned to immutable digests for reproducible builds.

# Stage 1: Build frontend SPA
FROM node:22.23.0-slim@sha256:d9f850096136edbc402debdd8729579a288aac64574ada0ff4db26b6ae58b0b2 AS frontend
ARG OPENJARVIS_LEADERBOARD_PUBLIC_ANON=

WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json frontend/.npmrc ./
RUN npm install --global "$(node -p 'require("./package.json").packageManager')" && \
    npm ci --ignore-scripts
COPY frontend/ .
RUN VITE_SUPABASE_ANON_KEY="${OPENJARVIS_LEADERBOARD_PUBLIC_ANON}" npm run build

# Stage 2: Build Python package
FROM python:3.12.16-slim-bookworm@sha256:935c3c5b12357a7e10caa9c1e6ca5692344d97cf229832ef584be2cec9fc03df AS builder

RUN apt-get update && \
    apt-get install -y --no-install-recommends build-essential ca-certificates curl && \
    rm -rf /var/lib/apt/lists/*

ENV PATH="/root/.cargo/bin:${PATH}"
RUN curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | \
        sh -s -- -y --profile minimal --default-toolchain none && \
    rustup toolchain install 1.89 --profile minimal && \
    rustup default 1.89

WORKDIR /app
COPY pyproject.toml uv.lock README.md ./
# Install the server and Google Gemini provider extras from the frozen lockfile.
RUN pip install --no-cache-dir uv && \
    uv export --frozen --no-dev --extra server --extra inference-google --no-emit-project > requirements.txt && \
    uv pip install --system --no-deps -r requirements.txt && \
    uv pip install --system --no-deps "maturin>=1.12.6,<2"

COPY src/ src/
COPY rust/ rust/
COPY scripts/install scripts/install
COPY deploy/windows deploy/windows
COPY configs/openjarvis/examples configs/openjarvis/examples
COPY --from=frontend /src/openjarvis/server/static src/openjarvis/server/static/

RUN uv pip install --system --no-deps . && \
    maturin build --release \
        --manifest-path rust/crates/openjarvis-python/Cargo.toml \
        --interpreter python3 \
        --out /tmp/openjarvis-rust-wheel && \
    uv pip install --system --no-deps /tmp/openjarvis-rust-wheel/*.whl && \
    python3 -c "import openjarvis_rust; print('openjarvis_rust ok')" && \
    python3 -m pip uninstall -y maturin && \
    rm -rf /tmp/openjarvis-rust-wheel rust

# Stage 3: Runtime
FROM python:3.12.16-slim-bookworm@sha256:935c3c5b12357a7e10caa9c1e6ca5692344d97cf229832ef584be2cec9fc03df
COPY --from=builder /usr/local /usr/local
COPY --from=builder /app /app
WORKDIR /app

RUN groupadd --system --gid 10001 openjarvis && \
    useradd --system --uid 10001 --gid openjarvis \
        --create-home --home-dir /home/openjarvis openjarvis
ENV HOME=/home/openjarvis
USER openjarvis

EXPOSE 8000
ENTRYPOINT ["jarvis"]
CMD ["serve", "--host", "0.0.0.0", "--port", "8000"]
