package com.example.ainovel.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.example.ainovel.model.CharacterCard;
import com.example.ainovel.model.CharacterChangeLog;
import com.example.ainovel.model.Manuscript;
import com.example.ainovel.model.OutlineCard;
import com.example.ainovel.model.OutlineChapter;
import com.example.ainovel.model.OutlineScene;
import com.example.ainovel.model.StoryCard;
import com.example.ainovel.model.User;

@DataJpaTest
class CharacterChangeLogRepositoryTest {

    @Autowired
    private CharacterChangeLogRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    private Manuscript manuscript;
    private OutlineScene scene;
    private CharacterCard characterA;
    private CharacterCard characterB;

    @BeforeEach
    void init() {
        User user = new User();
        user.setId(1L);
        user.setUsername("tester");
        user.setEmail("tester@example.com");
        user.setPassword("pass");
        entityManager.persist(user);

        StoryCard story = new StoryCard();
        story.setId(10L);
        story.setTitle("Story");
        story.setUser(user);
        entityManager.persist(story);

        OutlineCard outline = new OutlineCard();
        outline.setId(20L);
        outline.setStoryCard(story);
        outline.setUser(user);
        outline.setTitle("Outline");
        entityManager.persist(outline);

        OutlineChapter chapter = new OutlineChapter();
        chapter.setId(30L);
        chapter.setOutlineCard(outline);
        chapter.setChapterNumber(1);
        entityManager.persist(chapter);

        scene = new OutlineScene();
        scene.setId(40L);
        scene.setOutlineChapter(chapter);
        scene.setSceneNumber(2);
        entityManager.persist(scene);

        manuscript = new Manuscript();
        manuscript.setId(50L);
        manuscript.setOutlineCard(outline);
        manuscript.setUser(user);
        manuscript.setTitle("Manuscript");
        entityManager.persist(manuscript);

        characterA = new CharacterCard();
        characterA.setId(60L);
        characterA.setName("角色A");
        characterA.setStoryCard(story);
        characterA.setUser(user);
        entityManager.persist(characterA);

        characterB = new CharacterCard();
        characterB.setId(61L);
        characterB.setName("角色B");
        characterB.setStoryCard(story);
        characterB.setUser(user);
        entityManager.persist(characterB);
    }

    @Test
    void findByManuscriptAndSceneShouldIgnoreSoftDeleted() {
        CharacterChangeLog active = createLog(characterA, 1, 1, false);
        CharacterChangeLog deleted = createLog(characterB, 1, 1, false);
        deleted.setDeletedAt(LocalDateTime.now());
        entityManager.persist(active);
        entityManager.persist(deleted);
        entityManager.flush();

        List<CharacterChangeLog> logs = repository
                .findByManuscript_IdAndSceneIdAndDeletedAtIsNullOrderByCharacter_Id(manuscript.getId(), scene.getId());

        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getCharacter().getId()).isEqualTo(characterA.getId());
    }

    @Test
    void findFirstByCharacterShouldReturnLatestByChapterAndSection() {
        CharacterChangeLog first = createLog(characterA, 1, 1, false);
        CharacterChangeLog second = createLog(characterA, 2, 1, false);
        CharacterChangeLog latest = createLog(characterA, 2, 3, false);
        entityManager.persist(first);
        entityManager.persist(second);
        entityManager.persist(latest);
        entityManager.flush();

        CharacterChangeLog fetched = repository
                .findFirstByCharacter_IdAndManuscript_IdAndDeletedAtIsNullOrderByChapterNumberDescSectionNumberDescCreatedAtDesc(
                        characterA.getId(), manuscript.getId())
                .orElseThrow();

        assertThat(fetched.getSectionNumber()).isEqualTo(3);
    }

    @Test
    void findByCharacterShouldReturnAscendingOrder() {
        CharacterChangeLog first = createLog(characterB, 1, 3, false);
        CharacterChangeLog second = createLog(characterB, 1, 1, false);
        CharacterChangeLog third = createLog(characterB, 2, 1, false);
        entityManager.persist(first);
        entityManager.persist(second);
        entityManager.persist(third);
        entityManager.flush();

        List<CharacterChangeLog> logs = repository
                .findByCharacter_IdAndManuscript_IdAndDeletedAtIsNullOrderByChapterNumberAscSectionNumberAsc(
                        characterB.getId(), manuscript.getId());

        assertThat(logs).extracting(CharacterChangeLog::getSectionNumber)
                .containsExactly(1, 3, 1);
    }

    private CharacterChangeLog createLog(CharacterCard character, int chapterNumber, int sectionNumber, boolean autoCopied) {
        CharacterChangeLog log = new CharacterChangeLog();
        log.setCharacter(character);
        log.setManuscript(manuscript);
        log.setOutline(manuscript.getOutlineCard());
        log.setSceneId(scene.getId());
        log.setChapterNumber(chapterNumber);
        log.setSectionNumber(sectionNumber);
        log.setCharacterDetailsAfter("详情" + chapterNumber + sectionNumber);
        log.setIsAutoCopied(autoCopied);
        return log;
    }
}

