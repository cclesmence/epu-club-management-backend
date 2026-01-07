package com.sep490.backendclubmanagement.shared;

import lombok.AllArgsConstructor;
import org.modelmapper.Conditions;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.NameTokenizers;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class ModelMapperUtils {
    private static ModelMapper modelMapper;

    public static <T> T mapper(Object sourceData, Class<T> destinationClassType) {
        return getInstance().map(sourceData, destinationClassType);
    }

    public static <T> T mapper(Object sourceData, T targetData) {
        getInstance().map(sourceData, targetData);
        return targetData;
    }

    public static <S, T> List<T> mapList(List<S> source, Class<T> targetClass) {
        return source.stream().map(e -> getInstance().map(e, targetClass)).collect(Collectors.toList());
    }

    public static <S, T> Page<T> mapPage(Page<S> source, Class<T> targetClass) {
        return source.map(e -> getInstance().map(e, targetClass));
    }

    public static <T> T mapIfNotNull(Object sourceData, T targetData) {
        ModelMapper modelMapper = getInstance();
        modelMapper.getConfiguration().setPropertyCondition(Conditions.isNotNull());
        modelMapper.map(sourceData, targetData);
        return targetData;
    }

    private static ModelMapper getInstance() {
        if (modelMapper == null) {
            modelMapper = new ModelMapper();
            modelMapper.getConfiguration().setSourceNameTokenizer(NameTokenizers.UNDERSCORE);
            modelMapper.getConfiguration().setDestinationNameTokenizer(NameTokenizers.UNDERSCORE);
        }
        return modelMapper;
    }
}
