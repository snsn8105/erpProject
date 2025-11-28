package com.programpractice.approval_request_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import static org.springframework.data.mongodb.core.FindAndModifyOptions.options;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

// MongoDB Sequence 자동 증가 서비스
@Service
@RequiredArgsConstructor
public class SequenceGeneratorService {
    
    private final MongoOperations mongoOperations;
    
    // 다음 시퀀스 번호 생성
    public int generateSequence(String seqName) {
        DatabaseSequence counter = mongoOperations.findAndModify(
                query(where("_id").is(seqName)),
                new Update().inc("seq", 1),
                options().returnNew(true).upsert(true),
                DatabaseSequence.class
        );
        
        return counter != null ? counter.getSeq() : 1;
    }
}

/**
 * Sequence Document
 */
@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@org.springframework.data.mongodb.core.mapping.Document(collection = "sequences")
class DatabaseSequence {
    
    @org.springframework.data.annotation.Id
    private String id;
    
    private int seq;
}
