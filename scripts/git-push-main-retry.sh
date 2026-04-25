#!/usr/bin/env bash
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

max_attempts="${GIT_PUSH_MAX_ATTEMPTS:-5}"
for attempt in $(seq 1 "$max_attempts"); do
  git fetch origin "+refs/heads/master:refs/remotes/origin/master"
  git rebase --autostash origin/master
  if git push origin master --follow-tags; then
    exit 0
  fi
  echo "git push failed (attempt ${attempt}/${max_attempts}), retrying…" >&2
  sleep "$((attempt * 4))"
done
echo "git push failed after ${max_attempts} attempts" >&2
exit 1
