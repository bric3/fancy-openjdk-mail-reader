# Deploying to Google Cloud Run

This guide explains how to deploy Fancy Mail to Google Cloud Run using Jib.

## Prerequisites

1. **Google Cloud SDK** installed and configured
   ```bash
   # macOS
   brew install google-cloud-sdk

   # Authenticate
   gcloud auth login
   gcloud auth configure-docker
   ```

2. **A GCP Project** with billing enabled
   ```bash
   # Set your project
   gcloud config set project YOUR_PROJECT_ID
   ```

3. **Java 25** installed locally for building

## Deploying

### 1. Build and push the container image

```bash
./gradlew jib -PgcpProjectId=YOUR_PROJECT_ID
```

This builds and pushes the image directly to Google Container Registry without needing Docker installed.

### 2. Deploy to Cloud Run

```bash
gcloud run deploy fancy-mail \
  --image gcr.io/YOUR_PROJECT_ID/fancy-mail:latest \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --memory 512Mi
```

After deployment, you'll get a URL like `https://fancy-mail-xxxxx-uc.a.run.app`.

## Configuration Options

### Memory and CPU

```bash
--memory 512Mi    # Memory allocation (default: 512Mi)
--cpu 1           # CPU allocation (default: 1)
```

### Environment Variables

```bash
--set-env-vars "KEY=value,OTHER=value"
```

## Available Regions

Cloud Run is available in many regions. Choose one close to your users.

### Tier 1 Pricing (Lower cost)

| Region | Location | Low CO2 |
|--------|----------|---------|
| `us-central1` | Iowa | Yes |
| `us-east1` | South Carolina | |
| `us-east4` | Northern Virginia | |
| `us-east5` | Columbus | |
| `us-south1` | Dallas | Yes |
| `us-west1` | Oregon | Yes |
| `northamerica-south1` | Mexico | |
| `europe-north1` | Finland | Yes |
| `europe-north2` | Stockholm | Yes |
| `europe-west1` | Belgium | Yes |
| `europe-west4` | Netherlands | Yes |
| `europe-west8` | Milan | |
| `europe-west9` | Paris | Yes |
| `europe-southwest1` | Madrid | Yes |
| `asia-east1` | Taiwan | |
| `asia-northeast1` | Tokyo | |
| `asia-northeast2` | Osaka | |
| `asia-south1` | Mumbai | |
| `me-west1` | Tel Aviv | |

### Tier 2 Pricing (Higher cost)

| Region | Location | Low CO2 |
|--------|----------|---------|
| `us-west2` | Los Angeles | |
| `us-west3` | Salt Lake City | |
| `us-west4` | Las Vegas | |
| `northamerica-northeast1` | Montreal | Yes |
| `northamerica-northeast2` | Toronto | Yes |
| `southamerica-east1` | São Paulo | Yes |
| `southamerica-west1` | Santiago | Yes |
| `europe-west2` | London | Yes |
| `europe-west3` | Frankfurt | |
| `europe-west6` | Zurich | Yes |
| `europe-west10` | Berlin | |
| `europe-west12` | Turin | |
| `europe-central2` | Warsaw | |
| `asia-east2` | Hong Kong | |
| `asia-northeast3` | Seoul | |
| `asia-southeast1` | Singapore | |
| `asia-southeast2` | Jakarta | |
| `asia-south2` | Delhi | |
| `australia-southeast1` | Sydney | |
| `australia-southeast2` | Melbourne | |
| `me-central1` | Doha | |
| `me-central2` | Dammam | |
| `africa-south1` | Johannesburg | |

## Pricing

### Free Tier (Monthly, per billing account)

| Resource | Free Allowance |
|----------|----------------|
| CPU | 180,000 vCPU-seconds (~50 hours) |
| Memory | 360,000 GiB-seconds (~100 hours for 1GB container) |
| Requests | 2 million |
| Egress (North America) | 1 GB |

### Traffic/Egress Costs (beyond free tier)

| Destination | Cost per GB |
|-------------|-------------|
| Same region (to GCS, BigQuery, etc.) | Free |
| Within North America | ~$0.12 |
| To Europe | ~$0.12 |
| To Asia/Pacific | ~$0.12 |
| To other regions | Varies |

Ingress (incoming traffic) is always free.

### Compute Costs (beyond free tier)

| Resource | Tier 1 Regions | Tier 2 Regions |
|----------|----------------|----------------|
| CPU | $0.000024/vCPU-second | $0.0000312/vCPU-second |
| Memory | $0.0000025/GiB-second | $0.00000325/GiB-second |
| Requests | $0.40/million | $0.40/million |

## Local Testing

### Build to local Docker daemon

```bash
./gradlew jibDockerBuild
docker run -p 8080:8080 fancy-mail
```

### Run directly

```bash
./gradlew run --args="--server"
```

## Useful Commands

```bash
# List deployed services
gcloud run services list

# View logs
gcloud run services logs read fancy-mail --region us-central1

# Delete service
gcloud run services delete fancy-mail --region us-central1

# Update with new image
gcloud run deploy fancy-mail \
  --image gcr.io/YOUR_PROJECT_ID/fancy-mail:latest \
  --region us-central1
```

## References

- [Cloud Run Documentation](https://cloud.google.com/run/docs)
- [Cloud Run Pricing](https://cloud.google.com/run/pricing)
- [Cloud Run Locations](https://cloud.google.com/run/docs/locations)
- [Jib Gradle Plugin](https://github.com/GoogleContainerTools/jib/tree/master/jib-gradle-plugin)
