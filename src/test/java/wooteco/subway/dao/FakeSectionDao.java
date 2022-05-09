package wooteco.subway.dao;

import java.util.HashMap;
import java.util.Map;

import wooteco.subway.domain.Section;

public class FakeSectionDao implements SectionDao {

    private Long seq = 0L;
    private final Map<Long, Section> sections = new HashMap<>();

    @Override
    public Long save(Section section) {
        Section newSection = new Section(++seq, section.getLineId(), section.getUpStationId(), section.getDownStationId(),
            section.getDistance());
        sections.put(seq, newSection);
        return seq;
    }
}
