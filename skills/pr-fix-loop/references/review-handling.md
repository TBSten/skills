---
name: review-handling
description: >
  Detailed procedure for pr-fix-loop Step 5: fetching, fixing, and resolving review threads and
  issue-level comments, plus the handled-marker wrap convention. Inline review threads have
  GitHub's native resolve mechanism; conversation-level issue comments do not, so mark them handled
  by wrapping the body in a collapsible <details><summary>…</summary> and appending the fixing
  commit hash. Covers miss-prevention for both kinds and how to associate commits when handling
  several threads at once. Both APIs paginate — page through them.
---

# Review handling — pr-fix-loop Step 5

The detailed procedure invoked from `pr-fix-loop` Step 5. The parent `SKILL.md` only sketches it.

A PR has **two kinds** of comment, and pr-fix-loop must check **both**:

1. **inline review thread** (`gh api graphql ... reviewThreads { isResolved }`) — GitHub has a
   native resolve mechanism
2. **issue-level / conversation comment** (`gh api repos/.../issues/<pr>/comments`) — no resolve
   mechanism; "handled" is marked by a project convention

## 5-A. Inline review threads (`reviewThreads`)

A PR with ≥ 1 unresolved thread is delegated to a **fix-ci-pr-comments** skill if one exists.
Otherwise, summarize each thread's comment and handle one at a time (fix → commit → push → resolve
each with the `resolveReviewThread` mutation).

Fetch (paginate — one page returns at most `first`):

```bash
gh api graphql -f query='
  query($owner: String!, $repo: String!, $pr: Int!, $cursor: String){
    repository(owner: $owner, name: $repo){
      pullRequest(number: $pr){
        reviewThreads(first: 100, after: $cursor){
          pageInfo { hasNextPage endCursor }
          nodes { id isResolved path line comments(first: 1){ nodes { body } } }
        }
      }
    }
  }' -F owner="$OWNER" -F repo="$REPO" -F pr="$pr" -F cursor=null
# if hasNextPage is true, call again with $cursor=$endCursor
```

Pass owner / repo / pr as `-F` query variables (never string-interpolate them into the query — it
avoids `<OWNER>` / `<REPO>` placeholder mix-ups). "Unhandled" ⇔ `isResolved == false`.

Resolve mutation:

```bash
gh api graphql -f query='
  mutation { resolveReviewThread(input: {threadId: "THREAD_ID"}) { thread { isResolved } } }
'
```

When you handle several threads in one commit, **associate** them with the fix (don't bulk-resolve
everything blindly). Ideally write "handled in this commit" per thread; including the thread id in
the commit message is enough.

## 5-B. Issue-level comments (`issue_comments`)

GitHub has no native resolve mechanism here, so mark "handled" by **wrapping the body in a
collapsible `<details><summary>…</summary>` and appending `--> <commit-hash>`**. This is a
convention this skill establishes; the summary text is a project choice (e.g. `Resolved`, `done`,
or a localized label). Pick one and use it consistently as the handled-marker.

Fetch (paginate — REST default page size is 30):

```bash
gh api --paginate "repos/$OWNER/$REPO/issues/$pr/comments" \
  -q '.[] | select(.body | startswith("<details><summary>Resolved") | not) | {id, body: (.body | .[0:200])}'
```

Handle:

1. `gh api repos/$OWNER/$REPO/issues/comments/<id> -q .body` — get the original body
2. Understand it, implement the fix → commit → push
3. `PATCH` the body to wrap it:

```bash
NEW_BODY=$(printf '<details><summary>Resolved</summary>\n\n%s\n\n</details>\n\n--> %s' "$ORIG" "$COMMIT_HASH")
gh api -X PATCH "repos/$OWNER/$REPO/issues/comments/<id>" -f body="$NEW_BODY"
```

For a comment containing several independent points, split the fix into per-point commits and note
each in the summary (`<commit-A> (point 1)`, `<commit-B> (point 2)`).

## Miss-prevention

Every pass, fetch **all** issue comments with `gh api --paginate` (pagination is mandatory — one
page returns 30). If ≥ 1 comment lacks the handled-marker wrap, count it the **same as
`unresolved ≥ 1`** toward Step 7's "changed" verdict. Watching only inline review threads and
missing issue comments lets a maintainer's follow-up sit unhandled. Inline review threads also
require pagination: with `reviewThreads(first: 100, after: $cursor)`, keep paging while
`pageInfo.hasNextPage` is true.
