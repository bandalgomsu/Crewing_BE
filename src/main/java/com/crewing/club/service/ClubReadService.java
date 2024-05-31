package com.crewing.club.service;

import com.crewing.club.dto.ClubInfoResponse;
import com.crewing.club.dto.ClubListResponse;
import com.crewing.club.entity.Club;
import com.crewing.user.entity.User;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ClubReadService {
    ClubInfoResponse getClubInfo(Long clubId);
    ClubListResponse getAllClubInfo(Pageable pageable);
    ClubListResponse getAllFilterClubInfo(Pageable pageable,int category);
    ClubListResponse getAllSearchClubInfo(Pageable pageable,String search);
    ClubListResponse getAllStatusClubInfo(Pageable pageable, String status,User user);
}