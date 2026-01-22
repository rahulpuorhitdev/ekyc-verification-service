package org.example.ekyc.models;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.example.util.Utils.isBlank;

public record VerificationRequest (
        String requestID,
        Customer customer,
        List<VerificationType> verificationType,
        Instant timestamp,
        DocumentInput documentInput,
        FaceMatchInput faceMatchInput,
        AddressInput addressInput
){


    public record DocumentInput(
            String documentType,
            String documentNumber,
            LocalDate expiryDate,
            String documentImageUrl
    ){
        public DocumentInput{
            if( isBlank(documentType)){
                throw new IllegalArgumentException("Document type is required");
            }

            if( isBlank(documentNumber)){
                throw new IllegalArgumentException("Document number is required");
            }

            if( isBlank(documentImageUrl)){
                throw new IllegalArgumentException("Document image url is required");
            }

            if( expiryDate == null){
                throw new IllegalArgumentException("Expiry Date is required");
            }
        }
    }

    public record AddressInput(
            String proofType,
            LocalDate proofDate,
            String proofUrl
    ){
        public AddressInput{
            if( isBlank(proofType)){
                throw new IllegalArgumentException("Proof type is required");
            }

            if( isBlank(proofUrl)){
                throw new IllegalArgumentException("Proof url is required");
            }

            if( proofDate == null){
                throw new IllegalArgumentException("Proof date is required");
            }
        }
    }

    public record FaceMatchInput(
            String selfieURL,
            String idPhotoURL
    ){
        public FaceMatchInput{
            if( isBlank(selfieURL)){
                throw new IllegalArgumentException("Selfie URL is required");
            }

            if( isBlank(idPhotoURL)){
                throw new IllegalArgumentException("ID Photo URL is required");
            }
        }
    }
}
