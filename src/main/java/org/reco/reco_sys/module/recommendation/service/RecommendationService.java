package org.reco.reco_sys.module.recommendation.service;

import org.reco.reco_sys.module.recommendation.dto.RecommendResponse;

public interface RecommendationService {
    RecommendResponse recommend(Long userId, Long courseId);
    RecommendResponse getLatest(Long userId, Long courseId);
}
