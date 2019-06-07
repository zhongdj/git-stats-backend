# git-stats-backend
loading configuration, git repositories, generate raw data into database, and provide basic analysis reports

# Build Docker Image

```bash
sbt docker:publishLocal
```

# Start Backend Services
```bash
docker-compose up
```

# Note

In order to clone git repositories in name of you, docker will mount your ~/.ssh folder into container's /root/.ssh, 
and then create a ~/.git-stats-tasks folder on your host system to maintain analysis tasks. 