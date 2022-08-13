package com.github.ladynev.specification.lib.repository.support.test;

import com.github.ladynev.specification.lib.JpaSpecificationExecutorWithProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Created by pramoth on 9/28/2016 AD.
 */
public interface DocumentRepository extends JpaRepository<Document, Long>, JpaSpecificationExecutorWithProjection<Document, Long> {

    interface DocumentWithoutParent {

        Long getId();

        String getDescription();

        String getDocumentType();

        String getDocumentCategory();

        List<DocumentWithoutParent> getChild();

    }

    interface DocumentWithoutChild {

        Long getId();

        String getDescription();

        String getDocumentType();

        String getDocumentCategory();

    }

    interface OnlyId {

        Long getId();

    }

    interface OnlyParent extends OnlyId {

        OnlyId getParent();

    }

    interface OpenProjection extends OnlyId {

        String getDescription();

    }

    interface ThreeColumns {

        Long getId();

        String getDescription();

        String getDocumentType();

    }

}
