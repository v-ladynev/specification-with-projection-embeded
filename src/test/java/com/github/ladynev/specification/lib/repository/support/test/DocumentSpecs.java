package com.github.ladynev.specification.lib.repository.support.test;

import org.springframework.data.jpa.domain.Specification;

/**
 * Created by pramoth on 9/29/2016 AD.
 */
public class DocumentSpecs {

    public static Specification<Document> idEq(Long id) {
        return (root, query, cb) -> cb.equal(root.get("id"), id);
    }

    public static Specification<Document> descriptionLike(String descriptionLike) {
        return (root, query, cb) -> cb.like(root.get("description"), descriptionLike);
    }

}
