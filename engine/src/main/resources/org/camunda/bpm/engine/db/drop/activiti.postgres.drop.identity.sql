drop index ACT_IDX_MEMB_GROUP;
drop index ACT_IDX_MEMB_USER;

alter table ACT_ID_MEMBERSHIP 
    drop constraint ACT_FK_MEMB_GROUP;
    
alter table ACT_ID_MEMBERSHIP 
    drop constraint ACT_FK_MEMB_USER;

drop table ACT_ID_TENANT;
drop table ACT_ID_INFO;
drop table ACT_ID_GROUP;
drop table ACT_ID_MEMBERSHIP;
drop table ACT_ID_USER;