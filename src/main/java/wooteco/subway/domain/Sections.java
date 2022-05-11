package wooteco.subway.domain;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Sections {
    private static final int MIN_SIZE = 1;

    private List<Section> sections;

    private Sections(List<Section> sections) {
        this.sections = sections;
    }

    public static Sections of(Section section) {
        return new Sections(new LinkedList<>(List.of(section)));
    }

    public static Sections of(List<Section> sections) {
        Map<Station, Station> stations = sections.stream()
            .collect(Collectors.toMap(Section::getUpStation, Section::getDownStation));
        Station upStation = findUpStation(stations);
        List<Section> newSections = getSortedSections(sections, stations, upStation);

        return new Sections(newSections);
    }

    private static List<Section> getSortedSections(List<Section> sections, Map<Station, Station> stations,
        Station upStation) {
        List<Section> newSections = new LinkedList<>();
        while (stations.containsKey(upStation)) {
            final Station station = upStation;
            newSections.add(findSectionByUpStation(sections, station));
            upStation = stations.get(upStation);
        }
        return newSections;
    }

    private static Section findSectionByUpStation(List<Section> sections, Station station) {
        return sections.stream()
            .filter(section -> section.isUpStation(station))
            .findFirst()
            .get();
    }

    private static Station findUpStation(Map<Station, Station> stations) {
        return stations.keySet().stream()
            .filter(station -> !stations.containsValue(station))
            .findFirst()
            .get();
    }

    public Optional<Section> insert(Section section) {
        LinkedList<Section> flexibleSections = new LinkedList<>(this.sections);
        for (int i = 0; i < flexibleSections.size(); i++) {
            Section sectionInLine = flexibleSections.get(i);
            if (insertSection(section, flexibleSections, i, sectionInLine)) {
                return Optional.of(sectionInLine);
            }
        }

        if (flexibleSections.size() == sections.size()) {
            insertSectionSide(section, flexibleSections);
            return Optional.empty();
        }

        throw new IllegalArgumentException("구간을 추가할 수 없습니다.");
    }

    private boolean insertSection(Section section, LinkedList<Section> flexibleSections, int i, Section sectionInLine) {
        if (canInsertUpStation(section, sectionInLine) && !canInsertDownStation(section, sectionInLine)) {
            sectionInLine.updateUpStation(section.getDownStation(),
                sectionInLine.getDistance() - section.getDistance());
            flexibleSections.add(i, section);
            sections = flexibleSections;
            return true;
        }
        if (canInsertDownStation(section, sectionInLine) && !canInsertUpStation(section, sectionInLine)) {
            sectionInLine.updateDownStation(section.getUpStation(),
                sectionInLine.getDistance() - section.getDistance());
            flexibleSections.add(i + 1, section);
            sections = flexibleSections;
            return true;
        }

        return false;
    }

    private void insertSectionSide(Section section, LinkedList<Section> flexibleSections) {
        Section lastSection = sections.get(sections.size() - 1);
        if (lastSection.isDownStation(section.getUpStation())) {
            flexibleSections.addLast(section);
            sections = flexibleSections;
            return;
        }

        Section firstSection = sections.get(0);
        if (firstSection.isUpStation(section.getDownStation())) {
            flexibleSections.addFirst(section);
            sections = flexibleSections;
            return;
        }

        throw new IllegalArgumentException("구간을 추가할 수 없습니다.");
    }

    private boolean canInsertDownStation(Section section, Section sectionInLine) {
        return sectionInLine.isDownStation(section.getDownStation())
            && sectionInLine.isLongerThan(section.getDistance());
    }

    private boolean canInsertUpStation(Section section, Section sectionInLine) {
        return sectionInLine.isUpStation(section.getUpStation())
            && sectionInLine.isLongerThan(section.getDistance());
    }

    public UpdatedSection delete(Station station) {
        LinkedList<Section> flexibleSections = new LinkedList<>(this.sections);
        validateMinSize(flexibleSections);
        if (flexibleSections.get(0).isUpStation(station)) {
            return removeTopStation(flexibleSections);
        }

        int lastIndex = flexibleSections.size() - 1;
        if (flexibleSections.get(lastIndex).isDownStation(station)) {
            return removeBottomStation(flexibleSections, lastIndex);
        }

        for (int i = 0; i < flexibleSections.size(); i++) {
            Section leftSection = sections.get(i);
            if (leftSection.isDownStation(station) && i != lastIndex) {
                return removeMiddleStation(flexibleSections, i, leftSection);
            }
        }

        throw new IllegalArgumentException("삭제시에 오류가 발생했습니다.");
    }

    private UpdatedSection removeMiddleStation(LinkedList<Section> flexibleSections, int i, Section leftSection) {
        Section rightSection = sections.get(i + 1);
        leftSection.updateDownStation(rightSection.getDownStation(),
            leftSection.getDistance() + rightSection.getDistance());
        flexibleSections.remove(rightSection);
        sections = flexibleSections;
        return UpdatedSection.from(rightSection.getId(), leftSection);
    }

    private UpdatedSection removeBottomStation(LinkedList<Section> flexibleSections, int lastIndex) {
        Section section = flexibleSections.remove(lastIndex);
        sections = flexibleSections;
        return UpdatedSection.of(section.getId());
    }

    private UpdatedSection removeTopStation(LinkedList<Section> flexibleSections) {
        Section section = flexibleSections.remove(0);
        sections = flexibleSections;
        return UpdatedSection.of(section.getId());
    }

    private void validateMinSize(LinkedList<Section> flexibleSections) {
        if (flexibleSections.size() == MIN_SIZE) {
            throw new IllegalArgumentException("한개 남은 구간은 제거할 수 없습니다.");
        }
    }

    public List<Section> getSections() {
        return new LinkedList<>(sections);
    }

    public List<Station> getStations() {
        List<Station> stations = new ArrayList<>();
        for (Section section : sections) {
            stations.add(section.getUpStation());
            stations.add(section.getDownStation());
        }
        return stations.stream()
            .distinct()
            .collect(Collectors.toList());
    }
}