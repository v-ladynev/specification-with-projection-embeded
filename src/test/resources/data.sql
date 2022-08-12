insert into FORM_TYPE values('01', 'FT.01');
insert into FORM_TYPE values('04', 'FT.04');
insert into FORM_TYPE values('05', 'FT.05');
insert into FORM_TYPE values('06', 'FT.06');
insert into FORM_TYPE values('07', 'FT.07');
insert into FORM_TYPE values('08', 'FT.08');

insert into DOCUMENT (description, category, type, FLAG_HAS_SUB, FORM_TYPE_ID) values('descriptiontest', 'FT.01', 'document_type_a', 'false', '01');
insert into DOCUMENT (description, category, type, FLAG_HAS_SUB, FORM_TYPE_ID) values('description', 'FT.01', 'document_type_a', 'true', '01');
insert into DOCUMENT (description, category, type, FLAG_HAS_SUB, FORM_TYPE_ID) values('description2', 'FT.01', 'document_type_b', 'false', '01');
insert into DOCUMENT (description, category, type, FLAG_HAS_SUB, FORM_TYPE_ID) values('description3', 'FT.04', 'document_type_c', 'false', '04');
insert into DOCUMENT (description, category, type, FLAG_HAS_SUB, FORM_TYPE_ID) values('description4', 'FT.08', 'document_type_b', 'false', '08');
insert into DOCUMENT (description, category, type, FLAG_HAS_SUB, FORM_TYPE_ID) values('description5', 'FT.07', 'document_type_a', 'false', '07');
insert into DOCUMENT (description, category, type, parent_id, FLAG_HAS_SUB, FORM_TYPE_ID) values('description6', 'FT.01', 'document_type_a', 2, 'false', '01');
insert into DOCUMENT (description, category, type, FLAG_HAS_SUB, FORM_TYPE_ID) values('description7', 'FT.05', 'document_type_a', 'false', '05');
insert into DOCUMENT (description, category, type, FLAG_HAS_SUB, FORM_TYPE_ID) values('description8', 'FT.06', 'document_type_a', 'false', '06');
insert into DOCUMENT (description, category, type, FLAG_HAS_SUB, FORM_TYPE_ID) values('description9', 'FT.06', 'document_type_b', 'false', '06');
insert into DOCUMENT (description, category, type, FLAG_HAS_SUB, FORM_TYPE_ID) values('description10', 'FT.05', 'document_type_a', 'false', '05');
insert into DOCUMENT (description, category, type, FLAG_HAS_SUB, FORM_TYPE_ID) values('description11', 'FT.06', 'document_type_c', 'false', '06');
insert into DOCUMENT (description, category, type, FLAG_HAS_SUB, FORM_TYPE_ID) values('description12', 'FT.06', 'document_type_a', 'false', '06');
insert into DOCUMENT (description, category, type, FLAG_HAS_SUB, parent_id, FORM_TYPE_ID) values('', 'FT.01', 'document_type_a', 'false', 1, '01');
insert into DOCUMENT (description, category, type, FLAG_HAS_SUB, parent_id, FORM_TYPE_ID) values('', 'FT.01', 'document_type_b', 'false', 3, '01');
insert into DOCUMENT (description, category, type, FLAG_HAS_SUB, parent_id, FORM_TYPE_ID) values('', 'FT.04', 'document_type_c', 'false', 4, '04');
insert into DOCUMENT (description, category, type, FLAG_HAS_SUB, parent_id, FORM_TYPE_ID) values('', 'FT.08', 'document_type_b', 'false', 5, '08');
insert into DOCUMENT (description, category, type, FLAG_HAS_SUB, parent_id, FORM_TYPE_ID) values('', 'FT.07', 'document_type_a', 'false', 6, '07');
insert into DOCUMENT (description, category, type, FLAG_HAS_SUB, parent_id, FORM_TYPE_ID) values('', 'FT.05', 'document_type_a', 'false', 8, '05');
insert into DOCUMENT (description, category, type, FLAG_HAS_SUB, parent_id, FORM_TYPE_ID) values('', 'FT.06', 'document_type_a', 'false', 9, '06');
insert into DOCUMENT (description, category, type, FLAG_HAS_SUB, parent_id, FORM_TYPE_ID) values('', 'FT.06', 'document_type_b', 'false', 10, '06');
insert into DOCUMENT (description, category, type, FLAG_HAS_SUB, parent_id, FORM_TYPE_ID) values('', 'FT.05', 'document_type_a', 'false', 11, '05');
insert into DOCUMENT (description, category, type, FLAG_HAS_SUB, parent_id, FORM_TYPE_ID) values('', 'FT.06', 'document_type_c', 'false', 12, '06');
insert into DOCUMENT (description, category, type, FLAG_HAS_SUB, parent_id, FORM_TYPE_ID) values('', 'FT.06', 'document_type_a', 'false', 13, '06');
