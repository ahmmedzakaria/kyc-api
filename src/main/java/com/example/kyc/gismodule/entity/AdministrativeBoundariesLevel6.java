package com.example.kyc.gismodule.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import java.util.UUID;

@Entity
@Table(name = "administrative_boundaries_level_6", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdministrativeBoundariesLevel6 {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(name = "village_code")
    private String villageCode;

    @Column(name = "village_name")
    private String villageName;

    @Column(name = "union_id")
    private UUID unionId;

    @Column(name = "union_code")
    private String unionCode;

    @Column(name = "union_name")
    private String unionName;

    @Column(name = "sub_district_id")
    private UUID subDistrictId;

    @Column(name = "sub_district_code")
    private String subDistrictCode;

    @Column(name = "sub_district_name")
    private String subDistrictName;

    @Column(name = "district_id")
    private UUID districtId;

    @Column(name = "district_code")
    private String districtCode;

    @Column(name = "district_name")
    private String districtName;

    @Column(name = "division_id")
    private UUID divisionId;

    @Column(name = "division_code")
    private String divisionCode;

    @Column(name = "division_name")
    private String divisionName;

    @Column(name = "country_id")
    private UUID countryId;

    @Column(name = "country_code")
    private String countryCode;

    @Column(name = "country_name")
    private String countryName;

    @Column(name = "state_id")
    private UUID stateId;

    @Column(name = "state_code")
    private String stateCode;

    @Column(name = "state_name")
    private String stateName;

    @Column(name = "area_type")
    private String areaType;

    @Column(name = "source")
    private String source;

/*    // PostGIS geometry fields
    @Column(name = "center_point", columnDefinition = "geometry(Point,4326)")
    private Point centerPoint;

    @Column(name = "point", columnDefinition = "geometry(Point,4326)")
    private Point point;

    @Column(name = "wkb_geometry", columnDefinition = "geometry(Geometry,4326)")
    private Geometry wkbGeometry;

    */
}

