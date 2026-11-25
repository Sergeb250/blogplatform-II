### REST vs GraphQL (median of 30 requests)

| Scenario | REST requests | REST ms | REST bytes | GraphQL requests | GraphQL ms | GraphQL bytes |
|---|---:|---:|---:|---:|---:|---:|
| One post, all fields | 1 | 8.4 | 640 | 1 | 7.6 | 604 |
| 10 newest posts, all fields | 1 | 27.3 | 6122 | 1 | 27.0 | 6059 |
| 10 newest posts, only id + title | 1 | 24.5 | 6122 | 1 | 25.7 | 480 |
| Post + author + comments | 3 | 13.2 | 1214 | 1 | 9.8 | 255 |
| 10 posts + their authors | 11 | 102.9 | 7795 | 1 | 27.3 | 659 |

### API paging, sorting and searching (REST, median of 30 requests)

| Request | ms | bytes |
|---|---:|---:|
| `GET /api/posts?page=0&size=10` | 24.0 | 6122 |
| `GET /api/posts?page=100&size=10` | 29.4 | 6122 |
| `GET /api/posts?page=5000&size=10` | 435.5 | 6116 |
| `GET /api/posts?size=100` | 29.1 | 59692 |
| `GET /api/posts?sortBy=title&direction=ASC` | 23.8 | 6065 |
| `GET /api/posts?sortBy=rating` | 1739.2 | 6131 |
| `GET /api/posts?authorId=7` | 5.5 | 6097 |
| `GET /api/posts?tag=tag-5` | 137.6 | 6089 |
| `GET /api/posts/search?keyword=77777` | 1038.7 | 760 |
| `GET /api/posts/search?keyword=topic` | 158.9 | 6131 |
