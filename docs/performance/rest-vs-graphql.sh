#!/usr/bin/env bash
# =============================================================================
# REST vs GraphQL (and API-level paging / sorting / searching) timings.
#
# 1. Build the benchmark database once (100,000 posts):
#      ./mvnw test -Pbenchmark -DbenchmarkPosts=100000
# 2. Start the backend on it, with little logging:
#      DB_URL='jdbc:mysql://localhost:3306/bloggingPlatform_perf?serverTimezone=UTC' \
#      ./mvnw spring-boot:run \
#        -Dspring-boot.run.arguments="--logging.level.com.amali.blogPlatform=WARN --logging.level.org.springframework.jdbc.core.JdbcTemplate=INFO"
# 3. Run:  bash docs/performance/rest-vs-graphql.sh [runs]
#
# Every number is the MEDIAN of [runs] requests (default 30), after 5 warm-up requests.
# Time = curl's time_total (connect + server work + download), in milliseconds.
# =============================================================================
API=${API:-http://localhost:8080}
RUNS=${1:-30}

median() { sort -n | awk '{ v[NR] = $1 } END { printf "%.1f", v[int((NR + 1) / 2)] * 1000 }'; }

# total time of one or more REST GET requests made one after another (like a client building one screen)
rest_once() { local total=0; for url in "$@"; do
  t=$(curl -s -o /dev/null -w "%{time_total}" "$API$url"); total=$(awk -v a="$total" -v b="$t" 'BEGIN { print a + b }'); done; echo "$total"; }
rest_time() { for i in $(seq 1 5); do rest_once "$@" > /dev/null; done
              for i in $(seq 1 "$RUNS"); do rest_once "$@"; done | median; }
rest_bytes() { local total=0; for url in "$@"; do
  b=$(curl -s -o /dev/null -w "%{size_download}" "$API$url"); total=$((total + b)); done; echo "$total"; }

gql_once() { curl -s -o /dev/null -w "$2" -X POST "$API/graphql" -H 'Content-Type: application/json' -d "{\"query\": \"$1\"}"; }
gql_time() { for i in $(seq 1 5); do gql_once "$1" "%{time_total}\n" > /dev/null; done
             for i in $(seq 1 "$RUNS"); do gql_once "$1" "%{time_total}\n"; done | median; }
gql_bytes() { gql_once "$1" "%{size_download}"; }

ALL_FIELDS='id title content status authorId authorUsername tags { id name } commentCount averageRating createdAt updatedAt'

row() { # name, number of REST requests, REST urls..., -- , graphql query
  local name=$1 count=$2; shift 2; local urls=(); while [ "$1" != "--" ]; do urls+=("$1"); shift; done; shift; local q=$1
  printf "| %s | %s | %s | %s | 1 | %s | %s |\n" "$name" "$count" "$(rest_time "${urls[@]}")" "$(rest_bytes "${urls[@]}")" \
         "$(gql_time "$q")" "$(gql_bytes "$q")"
}

echo "### REST vs GraphQL (median of $RUNS requests)"
echo
echo "| Scenario | REST requests | REST ms | REST bytes | GraphQL requests | GraphQL ms | GraphQL bytes |"
echo "|---|---:|---:|---:|---:|---:|---:|"
row "One post, all fields" 1 "/api/posts/1" -- "{ post(id: \\\"1\\\") { $ALL_FIELDS } }"
row "10 newest posts, all fields" 1 "/api/posts?size=10" -- "{ posts(size: 10) { $ALL_FIELDS } }"
row "10 newest posts, only id + title" 1 "/api/posts?size=10" -- "{ posts(size: 10) { id title } }"
row "Post + author + comments" 3 "/api/posts/1" "/api/users/2" "/api/posts/1/comments" -- \
    "{ post(id: \\\"1\\\") { title author { username email } comments { username content } } }"
row "10 posts + their authors" 11 "/api/posts?size=10" "/api/users/1" "/api/users/2" "/api/users/3" "/api/users/4" "/api/users/5" \
    "/api/users/6" "/api/users/7" "/api/users/8" "/api/users/9" "/api/users/10" -- "{ posts(size: 10) { title author { username } } }"

echo
echo "### API paging, sorting and searching (REST, median of $RUNS requests)"
echo
echo "| Request | ms | bytes |"
echo "|---|---:|---:|"
for url in "/api/posts?page=0&size=10" "/api/posts?page=100&size=10" "/api/posts?page=5000&size=10" \
           "/api/posts?size=100" "/api/posts?sortBy=title&direction=ASC" "/api/posts?sortBy=rating" \
           "/api/posts?authorId=7" "/api/posts?tag=tag-5" "/api/posts/search?keyword=77777" "/api/posts/search?keyword=topic"; do
  printf "| \`GET %s\` | %s | %s |\n" "$url" "$(rest_time "$url")" "$(rest_bytes "$url")"
done
