package com.socialpath.service.impl;

import com.socialpath.entity.Country;
import com.socialpath.repository.CountryRepository;
import com.socialpath.service.CountryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CountryServiceImpl implements CountryService {

    private final CountryRepository countryRepository;

    @Override
    public List<Country> findAll() {
        return countryRepository.findAllByOrderByNameAsc();
    }

    @Override
    public boolean isValidCode(String code) {
        return code == null || code.isBlank() || countryRepository.existsById(code);
    }

    @Override
    public Country resolve(String code) {
        return code == null || code.isBlank() ? null : countryRepository.getReferenceById(code);
    }
}
