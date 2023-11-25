# Documentation for AWS Lambda with Protobuf and S3 Integration

## Overview

This documentation covers the AWS Lambda function `cs441lambdastoreS3`, designed to process game winners using gRPC's protobuf format and store results in an Amazon S3 bucket. The function is part of a larger system for the Policeman and Thief Graph Game, where it acts as a backend service to track game outcomes.

## Program Description

The Lambda function is written in Python and uses protobuf for efficient data serialization. It receives a base64-encoded gRPC protobuf message, decodes it to extract the winner information, updates the win counts in an S3 bucket, and then responds with an encoded protobuf message containing updated win statistics.

### Protobufs and Their Usage

Protobuf (Protocol Buffers) by Google is a method of serializing structured data, similar to JSON or XML but more efficient. In this Lambda function, protobufs are used for:

1. **Encoding and Decoding Data**: The game client sends and receives data in protobuf format, which is compact and efficient for transmission.
2. **WinnerRequest and WinnerResponse**: Two protobuf messages are defined - `WinnerRequest` for incoming data (game winner) and `WinnerResponse` for outgoing data (updated win counts).

### Building and Local Invocation

To build and test the Lambda function locally:

1. **Ensure Requirements**: AWS SAM CLI installed.
2. **Build the Function**: Navigate to the function's directory and build it using SAM CLI:
   ```bash
   sam build --use-container
   ```
3. **Test Locally**: Invoke the function locally with a test event:
   ```bash
   sam local invoke "LambdaFunction" -e event.json
   ```
    The test event is defined in `event.json` and contains a base64-encoded protobuf message.

### Deploying with SAM CLI

To deploy the function to AWS:

1 **Deploy the Function**:
   ```bash
   sam deploy --guided
   ```
   This can be done once you have built the function locally. The `samconfig.toml` file contains the configuration for the deployment.

### S3 Bucket Access

The SAM template has been modified to grant the Lambda function access to the S3 bucket `buckerforsimrank`. This is achieved through the `Policies` attribute in the `LambdaFunction` resource:

```yaml
Policies:
  - S3ReadPolicy:
      BucketName: buckerforsimrank
  - S3WritePolicy:
      BucketName: buckerforsimrank
```

These policies ensure the Lambda function has read and write permissions on the specified S3 bucket.
