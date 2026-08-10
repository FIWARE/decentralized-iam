#! /bin/bash
set -euo pipefail

BASE_BRANCH="master"

for chartDir in charts/*/; do
  chartName=$(basename "$chartDir")
  chartFile="${chartDir}Chart.yaml"

  depCount=$(yq e '.dependencies | length' "$chartFile")
  if [ "$depCount" = "0" ] || [ "$depCount" = "null" ]; then
    continue
  fi

  for i in $(seq 0 $((depCount - 1))); do
    depName=$(yq e ".dependencies[$i].name" "$chartFile")
    depRepo=$(yq e ".dependencies[$i].repository" "$chartFile")
    depVersion=$(yq e ".dependencies[$i].version" "$chartFile")

    if [ "$depRepo" = "null" ]; then
      echo "$depName ($chartName): no repository set, skipping"
      continue
    fi

    echo "$depName ($chartName): current=$depVersion repo=$depRepo"

    indexUrl="${depRepo%/}/index.yaml"
    latestVersion=$(curl -fsSL "$indexUrl" | yq e ".entries.\"${depName}\"[].version" - | sort -V | tail -n1)

    if [ -z "$latestVersion" ] || [ "$latestVersion" = "null" ]; then
      echo "$depName: could not resolve latest version from $indexUrl, skipping"
      continue
    fi

    if [ "$latestVersion" = "$depVersion" ]; then
      echo "$depName: up to date ($depVersion)"
      continue
    fi

    branch="bump/${chartName}-${depName}-${latestVersion}"

    if git ls-remote --exit-code --heads origin "$branch" >/dev/null 2>&1; then
      echo "$depName: branch $branch already exists, skipping"
      continue
    fi

    if [ "$(gh pr list --head "$branch" --state open --json number --jq 'length')" != "0" ]; then
      echo "$depName: PR for $branch already open, skipping"
      continue
    fi

    echo "$depName: bumping $depVersion -> $latestVersion"

    git checkout "$BASE_BRANCH"
    git checkout -b "$branch"

    yq e -i ".dependencies[$i].version = \"${latestVersion}\"" "$chartFile"

    git add "$chartFile"
    git commit -m "chore(deps): bump ${depName} to ${latestVersion} in ${chartName}"
    git push origin "$branch"

    gh pr create \
      --base "$BASE_BRANCH" \
      --head "$branch" \
      --title "chore(deps): bump ${depName} to ${latestVersion} in ${chartName}" \
      --body "Bumps the \`${depName}\` dependency of the \`${chartName}\` chart from \`${depVersion}\` to \`${latestVersion}\`." \
      --label patch

    git checkout "$BASE_BRANCH"
  done
done
