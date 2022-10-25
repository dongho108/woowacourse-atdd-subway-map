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

    public static Sections of(List<Section> sections) {
        Map<Station, Station> stations = sections.stream()
            .collect(Collectors.toMap(Section::getUpStation, Section::getDownStation));
        Station upStation = findUpStation(stations);
        List<Section> newSections = getSortedSections(sections, stations, upStation);

        return new Sections(newSections);
    }

    private static Station findUpStation(Map<Station, Station> stations) {
        return stations.keySet().stream()
            .filter(station -> !stations.containsValue(station))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("해당 구간을 찾을 수 없습니다."));
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
            .filter(section -> section.hasUpStation(station))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("해당 구간을 찾을 수 없습니다."));
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

    private boolean insertSection(Section section, LinkedList<Section> flexibleSections, int index, Section sectionInLine) {
        if (canInsertUpStation(section, sectionInLine) && !canInsertDownStation(section, sectionInLine)) {
            addSection(flexibleSections, index, section);
            sectionInLine.updateUpStation(section.getDownStation(),
                sectionInLine.getDistance() - section.getDistance());
            sections = flexibleSections;
            return true;
        }
        if (canInsertDownStation(section, sectionInLine) && !canInsertUpStation(section, sectionInLine)) {
            addSection(flexibleSections, index + 1, section);
            sectionInLine.updateDownStation(section.getUpStation(),
                sectionInLine.getDistance() - section.getDistance());
            sections = flexibleSections;
            return true;
        }

        return false;
    }

    private void addSection(LinkedList<Section> sections, int index, Section section) {
        checkContainsStation(section);
        sections.add(index, section);
    }

    private void checkContainsStation(Section section) {
        List<Station> stations = getStations();
        if (stations.contains(section.getUpStation()) && stations.contains(section.getDownStation())) {
            throw new IllegalArgumentException("이미 존재하는 상행선과 하행선은 구간에 추가할 수 없습니다.");
        }
    }

    private void insertSectionSide(Section section, LinkedList<Section> flexibleSections) {
        Section lastSection = sections.get(sections.size() - 1);
        if (lastSection.hasDownStation(section.getUpStation())) {
            addSection(flexibleSections, flexibleSections.size(), section);
            sections = flexibleSections;
            return;
        }

        Section firstSection = sections.get(0);
        if (firstSection.hasUpStation(section.getDownStation())) {
            addSection(flexibleSections, 0, section);
            sections = flexibleSections;
            return;
        }

        throw new IllegalArgumentException("구간을 추가할 수 없습니다.");
    }

    private boolean canInsertUpStation(Section section, Section sectionInLine) {
        return sectionInLine.hasUpStation(section.getUpStation())
            && sectionInLine.isLongerThan(section.getDistance());
    }

    private boolean canInsertDownStation(Section section, Section sectionInLine) {
        return sectionInLine.hasDownStation(section.getDownStation())
            && sectionInLine.isLongerThan(section.getDistance());
    }

    public UpdatedSection delete(Station station) {
        LinkedList<Section> flexibleSections = new LinkedList<>(this.sections);
        validateMinSize(flexibleSections);

        int lastIndex = flexibleSections.size() - 1;
        if (isTopStation(station, flexibleSections)) {
            return removeSideStation(flexibleSections, 0);
        }

        if (isBottomStation(station, flexibleSections, lastIndex)) {
            return removeSideStation(flexibleSections, lastIndex);
        }

        Optional<UpdatedSection> updatedSection = deleteMiddleSection(station, flexibleSections, lastIndex);
        if (updatedSection.isPresent()) {
            return updatedSection.get();
        }

        throw new IllegalArgumentException("해당 역이 구간에 존재하지 않습니다.");
    }

    private void validateMinSize(LinkedList<Section> flexibleSections) {
        if (flexibleSections.size() == MIN_SIZE) {
            throw new IllegalArgumentException("한개 남은 구간은 제거할 수 없습니다.");
        }
    }

    private boolean isTopStation(Station station, LinkedList<Section> flexibleSections) {
        return flexibleSections.get(0).hasUpStation(station);
    }

    private UpdatedSection removeSideStation(LinkedList<Section> flexibleSections, int index) {
        Section section = flexibleSections.remove(index);
        sections = flexibleSections;
        return UpdatedSection.of(section.getId());
    }

    private boolean isBottomStation(Station station, LinkedList<Section> flexibleSections, int lastIndex) {
        return flexibleSections.get(lastIndex).hasDownStation(station);
    }

    private Optional<UpdatedSection> deleteMiddleSection(Station station,
        LinkedList<Section> flexibleSections, int lastIndex) {
        for (int i = 0; i < lastIndex; i++) {
            Section leftSection = sections.get(i);
            if (leftSection.hasDownStation(station)) {
                return Optional.of(removeMiddleStation(flexibleSections, i, leftSection));
            }
        }
        return Optional.empty();
    }

    private UpdatedSection removeMiddleStation(LinkedList<Section> flexibleSections, int index, Section leftSection) {
        Section rightSection = sections.get(index + 1);
        leftSection.updateDownStation(rightSection.getDownStation(),
            leftSection.getDistance() + rightSection.getDistance());
        flexibleSections.remove(rightSection);
        sections = flexibleSections;
        return UpdatedSection.from(rightSection.getId(), leftSection);
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
