
git-ls-tree - List the contents of a tree object

```bash
git --git-dir='/Users/barry/.git-stats-tasks/2/Swordfish/.git' --work-tree='/Users/barry/.git-stats-tasks/2/Swordfish' ls-tree -r HEAD --name-only
```

```bash
Business/Archive/pom.xml
Business/Archive/src/main/application/META-INF/MANIFEST.MF
Business/Archive/src/main/application/META-INF/glassfish-application.xml
Business/Archive/src/main/scripts/create-mixture-additives.sql
Business/Archive/src/main/scripts/create-test-data.sql
Business/GUI/Web/nb-configuration.xml
Business/GUI/Web/pom.xml
Business/GUI/Web/src/main/java/net/madz/web/auth/LoginFailureBean.java
Business/GUI/Web/src/main/webapp/WEB-INF/glassfish-web.xml
Business/GUI/Web/src/main/webapp/WEB-INF/web.xml
```

```bash
git log -- pom.xml
```

```
commit c3e12d5f2c190a6a37829f07b83fb1c29a610ea0
Author: Barry Zhong <zhongdj@gmail.com>
Date:   Wed Oct 16 16:32:41 2013 +0800

    Fix POM dependencies incorrect scope issue

    After this fix the crmp.ear file won't include unneccessary files

commit 071545a74e32f0af10850c8d5a694f030b0a89ac
Author: Barry Zhong <zhongdj@gmail.com>
Date:   Sat Oct 12 17:55:03 2013 +0800

    Add two New Modules as Test Utilities

commit 99229ce56d00b224ffc02d8e3f5e4864c725b512
Author: Barry Zhong <zhongdj@gmail.com>
Date:   Sat Oct 12 11:06:14 2013 +0800

    Add Test Sample Code for Embeddable EJB test

commit 89017b92931d850935f18311245f514956a67451
Author: Barry Zhong <zhongdj@gmail.com>
Date:   Wed Oct 9 15:23:31 2013 +0800

    add new module TOBinding, ported from old projects

commit 0f15d4e0c91e70fad5501bdaa69be8cc725adcfb
Author: Barry Zhong <zhongdj@gmail.com>
Date:   Mon Sep 23 23:00:48 2013 +0800

    refactoring module structure

commit a04592248a0a235d8c9176b0a6c9f67cb2bbbdad
Author: Barry Zhong <zhongdj@gmail.com>
Date:   Sun Sep 22 14:18:35 2013 +0800

    add Entities in several modules

    1. add entities
    2. checked schema generated with eclipselink according to entities
    Notes on joins accoss composte primary key tables,especially with
    following scenario:
    T1.PK(TENANT_ID, ID),
    T2.PK(TENANT_ID, ID)

    Remember to use JoinColumn.insertable, JoinColumn.updatable attributes
    to avoid multiple update capability on relationship definition.

    On the other hand:
    a. JoinColumns setup for ManyToOne with Composite Key. Owning Side's
    TENANT_ID is maintained ONLY by the owning side table, any of the
    ManyToOne relationship cannot "insert" or domain"update" owningSideTable's
    TENANT_ID
    b. JoinColumns setup for ManyToMany with Composite Key, which creates
    JoinTable. ONLY Owning side maintains the JOIN TABLE's TENANT_ID.


```

Raw data extraction
1. list all objects on the ls-tree. (object has it's full path as shown above)
2. foreach object parse all the commits, and extract commit-id, author, date, and message

Analysis
1. Tag objects with patterns, such as domain, application, infrastructure, 
   controller(api definition), message definition, message consumer, gateway, repository
2. Correlate tags from object to commit-id  
3. SELECT date, count(commit-id) FROM tagged_commit WHERE tag = 'domain' GROUP BY date ORDER BY date;  

