#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 3 ]]; then
    echo "Usage: $0 IMAGE_NAMESPACE IMAGE_TAG OUTPUT_DIRECTORY" >&2
    exit 2
fi

image_namespace=$1
image_tag=$2
output_directory=$3

if [[ ! $image_namespace =~ ^[a-z0-9]+([._-][a-z0-9]+)*$ ]]; then
    echo "ERROR: Invalid Docker image namespace '$image_namespace'." >&2
    exit 2
fi
if [[ ! $image_tag =~ ^[a-zA-Z0-9_][a-zA-Z0-9_.-]{0,127}$ ]]; then
    echo "ERROR: Invalid Docker image tag '$image_tag'." >&2
    exit 2
fi
if [[ ! $output_directory =~ ^k8s-deploy-[a-zA-Z0-9_.-]+$ ]]; then
    echo "ERROR: Output directory must be named k8s-deploy-*." >&2
    exit 2
fi

repository_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
manifest_directory="$repository_root/k8s"
output_path="$repository_root/$output_directory"
manifest_files=(04-discovery.yaml 05-services.yaml 06-frontend.yaml)
images=(
    discoveryservice
    accountservice
    customer-service
    identity-service
    swiggy-gateway
    bank-frontend
)

for manifest in "${manifest_files[@]}"; do
    if [[ ! -f "$manifest_directory/$manifest" ]]; then
        echo "ERROR: Required Kubernetes manifest '$manifest' is missing." >&2
        exit 1
    fi
done

for image in "${images[@]}"; do
    expected="rishuraj1/$image:latest"
    if ! grep -Fq -- "$expected" "${manifest_files[@]/#/$manifest_directory/}"; then
        echo "ERROR: Kubernetes manifests are missing expected image reference '$expected'." >&2
        exit 1
    fi
done

if [[ -e "$output_path" ]]; then
    echo "ERROR: Output directory '$output_directory' already exists." >&2
    exit 1
fi

mkdir -- "$output_path"
for manifest in "${manifest_files[@]}"; do
    cp -- "$manifest_directory/$manifest" "$output_path/$manifest"
    for image in "${images[@]}"; do
        expected="rishuraj1/$image:latest"
        replacement="$image_namespace/$image:$image_tag"
        expected_pattern=${expected//./\\.}
        sed -i "s|$expected_pattern|$replacement|g" "$output_path/$manifest"
    done
done
