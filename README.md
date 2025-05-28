## Street Sign Text Extraction – Bachelor’s Thesis
### Overview

This project was developed as part of a Bachelor’s Thesis and focuses on evaluating the accuracy and performance of various computer vision and image processing APIs on the Android platform. The main objective is to compare different techniques for text extraction from street sign images, using both local and cloud-based solutions.
Technologies Used

- **OpenCV –** For image preprocessing and enhancement.

- **Tesseract OCR –** Open-source optical character recognition engine for local text extraction.

- **Google Cloud Vision API –** Cloud-based OCR service for comparison with local solutions.

- **Android (Java) –** Platform for implementation and performance testing.

### Key Features

- Capture and process street sign images directly from an Android device

- Apply multiple preprocessing techniques (grayscale, thresholding, denoising, etc.)

- Perform OCR using different tools (Tesseract, Cloud Vision)

- Log and analyze the performance (latency) and precision (text recognition accuracy) of each method

- Visual comparison of OCR results from each approach

### Goals

- Measure and compare the efficiency (execution time, resource usage) of local vs. cloud-based OCR approaches

- Assess the accuracy of text recognition across varying image conditions (lighting, angle, quality)

- Determine the most effective pipeline for extracting text from real-world street signs

### Results & Analysis

The project includes performance benchmarks and accuracy metrics collected from multiple test scenarios. Results demonstrate the trade-offs between offline processing capabilities and cloud-based precision.
