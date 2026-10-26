# GitHub repository setup

Requested repository: QuietGate, private initially.

The source tree is prepared for GitHub. A GitHub connection must grant access to the destination repository. The available connector can read/write repository content but may not create a repository or upload release binary assets; those steps can use GitHub's UI or an authenticated Git CLI.

Never upload real call catalogs, contact exports, API credentials, debug logs containing personal data, signing keys, local.properties, build caches or SDK paths. The release source archive excludes these generated/local artifacts. APKs belong in release assets, not source history.

If creating the repository manually, choose Private and leave initialization empty when pushing an existing local repository. Use your GitHub account's private/noreply commit identity, or a generic contributor identity, according to your preference. Authentication should use the browser or a credential manager; do not paste a token into chat or source files.

The source uses MIT. Keeping the repository private does not make source publicly obtainable; public distribution is a separate later choice.
