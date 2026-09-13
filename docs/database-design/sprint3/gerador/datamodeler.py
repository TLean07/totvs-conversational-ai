# -*- coding: utf-8 -*-
"""
Gera um design do Oracle SQL Developer Data Modeler (arquivo .dmd + pasta) com o modelo
relacional completo e o diagrama ja posicionado (mesmo layout das imagens do documento).
"""
import os
import re
import shutil
import uuid
from datetime import datetime, timezone

import layout
from modelo import TABELAS, SEQUENCIAS, tabela, coluna

AUTOR = "Company 404"
SITE_ID = "01F6E6D1-21D0-CFBD-ABDD-3988D1813575"    # Oracle Database 21c (site padrao do Data Modeler 24.3)
SITE_NOME = "Oracle Database 21c"
SITE_TIPO = "17"

LOGDT = {"VARCHAR2": "LOGDT024", "CLOB": "LOGDT028", "NUMBER": "LOGDT019", "TIMESTAMP": "LOGDT015", "FLOAT": "LOGDT021"}


def gid():
    return str(uuid.uuid4()).upper()


def curto(id_):
    return id_[:8] + "-" + id_[-12:]


def agora():
    return datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")


def cab(nome_design):
    return "<createdBy>%s</createdBy>\n<createdTime>%s</createdTime>\n<ownerDesignName>%s</ownerDesignName>\n" % (AUTOR, agora(), nome_design)


def cdata(s):
    return "<![CDATA[%s]]>" % s


def parse_tipo(c):
    """Devolve (logicalDatatype, size, precision, scale)."""
    if c["identity"]:
        return LOGDT["NUMBER"], None, 19, 0
    m = re.match(r"([A-Z2]+)(?:\((.*)\))?", c["tipo"])
    base, args = m.group(1), m.group(2)
    if base == "VARCHAR2":
        return LOGDT["VARCHAR2"], args.strip(), None, None
    if base == "CLOB":
        return LOGDT["CLOB"], None, None, None
    if base == "NUMBER":
        p, s = [a.strip() for a in args.split(",")]
        return LOGDT["NUMBER"], None, int(p), int(s)
    if base == "TIMESTAMP":
        return LOGDT["TIMESTAMP"], None, int(args), None
    if base == "FLOAT":
        return LOGDT["FLOAT"], None, int(args), None
    raise ValueError(c["tipo"])


class Design:
    def __init__(self, nome):
        self.nome = nome
        self.design_id = gid()
        self.rel_id = gid()
        self.main_view = gid()
        self.logical_id = gid()
        self.logical_view = gid()
        self.tab_ids = {t["nome"]: gid() for t in TABELAS}
        self.col_ids = {(t["nome"], c["nome"]): gid() for t in TABELAS for c in t["colunas"]}
        self.pk_ids = {t["nome"]: gid() for t in TABELAS}
        self.uk_ids = {(t["nome"], uk["nome"]): gid() for t in TABELAS for uk in t["uks"]}
        self.fk_ids = {(t["nome"], fk["nome"]): gid() for t in TABELAS for fk in t["fks"]}
        self.fk_idx_ids = {(t["nome"], fk["nome"]): gid() for t in TABELAS for fk in t["fks"]}
        self.view_ids = {t["nome"]: gid() for t in TABELAS}
        self.seq_ids = {s["nome"]: gid() for s in SEQUENCIAS}
        self.dominios = {}   # (logdt, size, prec, scale) -> (id, nome)

    def dominio(self, logdt, size, prec, scale):
        chave = (logdt, size, prec, scale)
        if chave not in self.dominios:
            base = {"LOGDT024": "VARCHAR", "LOGDT028": "CLOB", "LOGDT019": "NUMERIC", "LOGDT015": "Timestamp", "LOGDT021": "FLOAT"}[logdt]
            nome = "%s_%s_%s_%s" % (base, prec if prec is not None else 0, scale if scale is not None else 0, (size or "0").replace(" ", "_"))
            self.dominios[chave] = (gid(), nome)
        return self.dominios[chave][0]

    def xml_dominios(self):
        out = ["<?xml version = '1.0' encoding = 'UTF-8'?>",
               '<DomainFile class="oracle.dbtools.crest.model.design.DomainFileWrapper" fileName="%s_Domains">' % self.nome, "<domains>"]
        for (logdt, size, prec, scale), (id_, nome) in self.dominios.items():
            out.append('<Domain class="oracle.dbtools.crest.model.design.Domain" name="%s" id="%s">' % (nome, id_))
            out.append(cab(self.nome).rstrip("\n"))
            out.append("<avTSortOrder>0</avTSortOrder>")
            if prec is not None:
                out.append("<dataTypePrecision>%s</dataTypePrecision>" % prec)
            if scale is not None:
                out.append("<dataTypeScale>%s</dataTypeScale>" % scale)
            if size is not None:
                out.append("<dataTypeSize>%s</dataTypeSize>" % size)
            out.append("<fileName>%s_Domains</fileName>" % self.nome)
            out.append("<logicalDatatype>%s</logicalDatatype>" % logdt)
            out.append("</Domain>")
        out.append("</domains>\n</DomainFile>")
        return "\n".join(out) + "\n"

    # ------------------------------------------------------------------ tabela
    def xml_tabela(self, t):
        n = t["nome"]
        out = ['<?xml version = \'1.0\' encoding = \'UTF-8\'?>',
               '<Table class="oracle.dbtools.crest.model.design.relational.Table" directorySegmentName="seg_0" id="%s" name="%s">' % (self.tab_ids[n], n),
               cab(self.nome).rstrip("\n"),
               "<commentInRDBMS>%s</commentInRDBMS>" % cdata(t["comentario"]),
               "<allowColumnReorder>false</allowColumnReorder>",
               "<existDependencyGenerateInDDl>true</existDependencyGenerateInDDl>",
               '<columns itemClass="oracle.dbtools.crest.model.design.relational.Column">']
        for c in t["colunas"]:
            out.append(self.xml_coluna(t, c))
        out.append("</columns>")
        out.append('<indexes itemClass="oracle.dbtools.crest.model.design.relational.Index">')
        out.append(self.xml_indice(t, self.pk_ids[n], t["pk"]["nome"], t["pk"]["colunas"], "Primary Constraint", pk=True))
        for uk in t["uks"]:
            out.append(self.xml_indice(t, self.uk_ids[(n, uk["nome"])], uk["nome"], uk["colunas"], "Unique Constraint"))
        for fk in t["fks"]:
            out.append(self.xml_indice(t, self.fk_idx_ids[(n, fk["nome"])], fk["nome"], fk["colunas"], "Foreign Key"))
        out.append("</indexes>")
        checks = [(("CK_%s_%s" % (n, c["nome"]))[:30], c["check"]) for c in t["colunas"] if c["check"]]
        checks += [(ck["nome"], ck["regra"]) for ck in t["checks"]]
        if checks:
            out.append('<tableCheckConstraints itemClass="oracle.dbtools.crest.model.design.constraint.TableLevelConstraint">')
            for nome, regra in checks:
                out.append('<tableCheckConstraint name="%s" directorySegmentName="seg_0" id="%s">' % (nome, gid()))
                out.append(cab(self.nome).rstrip("\n"))
                out.append("<rule>%s</rule>" % cdata(regra))
                out.append("</tableCheckConstraint>")
            out.append("</tableCheckConstraints>")
        out.append("</Table>")
        return "\n".join(out) + "\n"

    def xml_coluna(self, t, c):
        n = t["nome"]
        logdt, size, prec, scale = parse_tipo(c)
        out = ['<Column name="%s" id="%s">' % (c["nome"], self.col_ids[(n, c["nome"])]),
               cab(self.nome).rstrip("\n"),
               "<commentInRDBMS>%s</commentInRDBMS>" % cdata(c["comentario"])]
        if c["nulo"]:
            out.append("<nullsAllowed>true</nullsAllowed>")
        if c["default"] and not c["identity"]:
            out.append("<defaultValue>%s</defaultValue>" % c["default"])
        if c["identity"]:
            out.append("<autoIncrementColumn>true</autoIncrementColumn>")
            out.append("<identityColumn>true</identityColumn>")
            out.append("<autoIncrementGenerateTrigger>false</autoIncrementGenerateTrigger>")
        out.append("<use>0</use>")
        out.append("<logicalDatatype>%s</logicalDatatype>" % logdt)
        out.append("<domain>%s</domain>" % self.dominio(logdt, size, prec, scale))
        out.append("<ownDataTypeParameters>,,</ownDataTypeParameters>")
        out.append("<autoIncrementCycle>false</autoIncrementCycle>")
        assoc = []
        for fk in t["fks"]:
            if c["nome"] in fk["colunas"]:
                pai = fk["ref"]
                ref_col = fk["ref_colunas"][fk["colunas"].index(c["nome"])]
                assoc.append('<colAssociation fkAssociation="%s" referredColumn="%s"/>' % (
                    self.fk_ids[(n, fk["nome"])], self.col_ids[(pai, ref_col)]))
        if assoc:
            out.append("<associations>\n%s\n</associations>" % "\n".join(assoc))
        out.append("</Column>")
        return "\n".join(out)

    def xml_indice(self, t, id_, nome, cols, estado, pk=False):
        out = ['<ind_PK_UK id="%s" name="%s">' % (id_, nome), cab(self.nome).rstrip("\n")]
        if pk:
            out.append("<pk>true</pk>")
        out.append("<indexState>%s</indexState>" % estado)
        out.append("<isSurrogateKey>false</isSurrogateKey>")
        out.append("<indexColumnUsage>")
        for c in cols:
            out.append('<colUsage columnID="%s"/>' % self.col_ids[(t["nome"], c)])
        out.append("</indexColumnUsage>")
        out.append("</ind_PK_UK>")
        return "\n".join(out)

    # ------------------------------------------------------------------ FK
    def xml_fk(self, t, fk):
        n = t["nome"]
        pai = fk["ref"]
        obrig = all(not coluna(t, c)["nulo"] for c in fk["colunas"])
        regra = {"CASCADE": "CASCADE", "SET NULL": "SET NULL", None: "NO ACTION"}[fk["on_delete"]]
        return "\n".join([
            '<?xml version = \'1.0\' encoding = \'UTF-8\'?>',
            '<FKIndexAssociation class="oracle.dbtools.crest.model.design.relational.FKIndexAssociation" directorySegmentName="seg_0" id="%s" containerWithKeyObject="%s" localFKIndex="%s" name="%s">' % (
                self.fk_ids[(n, fk["nome"])], self.tab_ids[n], self.fk_idx_ids[(n, fk["nome"])], fk["nome"]),
            cab(self.nome).rstrip("\n"),
            "<referredTableLongName>%s</referredTableLongName>" % pai,
            "<deleteRule>%s</deleteRule>" % regra,
            "<referredTableID>%s</referredTableID>" % self.tab_ids[pai],
            "<keyObject>%s</keyObject>" % self.pk_ids[pai],
            "<referredKeyID>%s</referredKeyID>" % self.pk_ids[pai],
            "<mandatory>%s</mandatory>" % ("true" if obrig else "false"),
            "<refColNames>%s</refColNames>" % ",".join(fk["ref_colunas"]),
            "<transferable>true</transferable>",
            "<rely>false</rely>",
            "<columnDependencyConstraintGenerateInDDL>true</columnDependencyConstraintGenerateInDDL>",
            "</FKIndexAssociation>",
        ]) + "\n"

    # ------------------------------------------------------------------ diagrama
    def xml_subview(self, lay):
        out = ['<?xml version = \'1.0\' encoding = \'UTF-8\'?>',
               '<Diagram class="oracle.dbtools.crest.swingui.relational.DPVRelational" name="Relational_1" id="%s">' % self.main_view,
               "<createdBy>%s</createdBy>\n<modifiedBy>%s</modifiedBy>\n<createdTime>%s</createdTime>" % (AUTOR, AUTOR, agora()),
               "<autoRoute>false</autoRoute>\n<boxInbox>true</boxInbox>\n<showLegend>false</showLegend>\n<showLabels>true</showLabels>",
               "<showRelAttributes>false</showRelAttributes>\n<gridEnabled>false</gridEnabled>\n<showGrid>false</showGrid>",
               "<diagramColor>-1</diagramColor>\n<display>false</display>\n<notation>1</notation>",
               "<objectViews>"]
        for nome, cx in lay["caixas"].items():
            out.append('<OView class="oracle.dbtools.crest.swingui.relational.TVTable" oid="%s" otype="Table" vid="%s">' % (self.tab_ids[nome], self.view_ids[nome]))
            out.append("<hideElementsInCurrentOnly>false</hideElementsInCurrentOnly>")
            out.append('<bounds x="%d" y="%d" width="%d" height="%d"/>' % (cx["x"], cx["y"], cx["w"], cx["h"]))
            out.append("</OView>")
        out.append("</objectViews>\n<connectors>")
        for con in lay["conectores"]:
            fk = con["fk"]
            out.append('<Connector class="oracle.dbtools.crest.swingui.relational.TVFKRelation" oid="%s" otype="FKIndexAssociation" vid_source="%s" vid_target="%s">' % (
                self.fk_ids[(con["filho"], fk["nome"])], self.view_ids[con["pai"]], self.view_ids[con["filho"]]))
            out.append("<hideElementsInCurrentOnly>false</hideElementsInCurrentOnly>\n<backgroundColor>-16777216</backgroundColor>\n<lineWidth>1</lineWidth>\n<points>")
            for x, y in reversed(con["pontos"]):   # do pai para o filho
                out.append('<point x="%d" y="%d"/>' % (x, y))
            out.append("</points>\n</Connector>")
        out.append("</connectors>\n</Diagram>")
        return "\n".join(out) + "\n"

    # ------------------------------------------------------------------ arquivos
    def escrever(self, pasta_saida, template):
        nome = self.nome
        raiz = os.path.join(pasta_saida, nome)
        if os.path.exists(raiz):
            shutil.rmtree(raiz)
        os.makedirs(raiz)
        dmd = os.path.join(pasta_saida, nome + ".dmd")
        with open(dmd, "w", encoding="utf-8", newline="\n") as f:
            f.write("<?xml version = '1.0' encoding = 'UTF-8'?>\n"
                    '<OSDM_Design class="oracle.dbtools.crest.model.design.Design" name="%s" id="%s" version="3.5">\n' % (nome, self.design_id)
                    + cab(nome) + "<capitalNames>false</capitalNames>\n<designId>%s</designId>\n</OSDM_Design>" % self.design_id)

        # boilerplate copiado do template (business info, process model, datatypes, settings)
        for sub in ("businessinfo", "pm", "datatypes"):
            shutil.copytree(os.path.join(template, sub), os.path.join(raiz, sub))
        for pasta, _, arquivos in os.walk(raiz):
            for a in arquivos:
                p = os.path.join(pasta, a)
                with open(p, "r", encoding="utf-8") as f:
                    s = f.read()
                s = s.replace("<ownerDesignName>GS</ownerDesignName>", "<ownerDesignName>%s</ownerDesignName>" % nome)
                with open(p, "w", encoding="utf-8", newline="\n") as f:
                    f.write(s)
        shutil.copy(os.path.join(template, "dl_settings.xml"), os.path.join(raiz, "dl_settings.xml"))
        os.makedirs(os.path.join(raiz, "domains"))
        os.makedirs(os.path.join(raiz, "files"))
        os.makedirs(os.path.join(raiz, "mapping"))
        with open(os.path.join(raiz, "mapping", "ExtendedMap.xml"), "w", encoding="utf-8", newline="\n") as f:
            f.write("<?xml version = '1.0' encoding = 'UTF-8'?>\n<ExtendedMap class=\"oracle.dbtools.crest.model.xtdmapping.ExtendedMap\">\n</ExtendedMap>")
        os.makedirs(os.path.join(raiz, "rdbms"))
        with open(os.path.join(raiz, "rdbms", nome + "_RDBMSSites.xml"), "w", encoding="utf-8", newline="\n") as f:
            f.write("<?xml version = '1.0' encoding = 'UTF-8'?>\n<metadatadoc version=\"2.0\"/>")
        with open(os.path.join(raiz, "ChangeRequests.local"), "w", encoding="utf-8", newline="\n") as f:
            f.write("<?xml version = '1.0' encoding = 'UTF-8'?>\n<changeRequests/>")
        with open(os.path.join(raiz, "dmd_open.local"), "w", encoding="utf-8", newline="\n") as f:
            f.write('<?xml version="1.0" encoding="UTF-8" ?>\n<design id="%s">\n\t<models>\n\t\t<model id="%s" open="true" open_d_pm="false"/>\n\t</models>\n</design>' % (self.design_id, self.rel_id))

        # modelo logico vazio
        lg = os.path.join(raiz, "logical")
        os.makedirs(os.path.join(lg, "subviews"))
        with open(os.path.join(lg, "Logical.xml"), "w", encoding="utf-8", newline="\n") as f:
            f.write("<?xml version = '1.0' encoding = 'UTF-8'?>\n"
                    '<LogicalDesign class="oracle.dbtools.crest.model.design.logical.LogicalDesign" name="Logical" id="%s" mainViewID="%s">\n' % (self.logical_id, self.logical_view)
                    + cab(nome) + "<shouldBeOpen>false</shouldBeOpen>\n</LogicalDesign>")
        with open(os.path.join(lg, "Objects.local"), "w", encoding="utf-8", newline="\n") as f:
            f.write("<?xml version = '1.0' encoding = 'UTF-8'?>\n<oracle.dbtools.crest.model.design.logical.LogicalDesign>\n</oracle.dbtools.crest.model.design.logical.LogicalDesign>")
        with open(os.path.join(lg, "Diagrams.local"), "w", encoding="utf-8", newline="\n") as f:
            f.write("<?xml version = '1.0' encoding = 'UTF-8'?>\n<oracle.dbtools.crest.model.design.logical.LogicalDesign>\n   <object objectType=\"main_view\" objectID=\"%s\" name=\"Logical\" visible=\"true\"/>\n</oracle.dbtools.crest.model.design.logical.LogicalDesign>" % self.logical_view)
        with open(os.path.join(lg, "subviews", self.logical_view + ".xml"), "w", encoding="utf-8", newline="\n") as f:
            f.write("<?xml version = '1.0' encoding = 'UTF-8'?>\n"
                    '<Diagram class="oracle.dbtools.crest.swingui.logical.DPVLogical" name="Logical" id="%s">\n' % self.logical_view
                    + "<createdBy>%s</createdBy>\n<modifiedBy>%s</modifiedBy>\n<createdTime>%s</createdTime>\n" % (AUTOR, AUTOR, agora())
                    + "<autoRoute>false</autoRoute>\n<boxInbox>true</boxInbox>\n<showLegend>false</showLegend>\n<showLabels>false</showLabels>\n<showRelAttributes>false</showRelAttributes>\n<gridEnabled>false</gridEnabled>\n<showGrid>false</showGrid>\n<diagramColor>-1</diagramColor>\n<display>false</display>\n<notation>1</notation>\n<objectViews>\n</objectViews>\n<connectors>\n</connectors>\n</Diagram>")

        # modelo relacional
        rel = os.path.join(raiz, "rel")
        os.makedirs(rel)
        with open(os.path.join(rel, curto(self.rel_id) + ".xml"), "w", encoding="utf-8", newline="\n") as f:
            f.write("<?xml version = '1.0' encoding = 'UTF-8'?>\n"
                    '<relationalModel class="oracle.dbtools.crest.model.design.relational.RelationalDesign" name="Relational_1" id="%s" mainViewID="%s">\n' % (self.rel_id, self.main_view)
                    + cab(nome) + "<shouldBeOpen>true</shouldBeOpen>\n<selectedRDBMSSite>%s</selectedRDBMSSite>\n</relationalModel>" % SITE_ID)
        rd = os.path.join(rel, curto(self.rel_id))
        for sub in ("table/seg_0", "foreignkey/seg_0", "subviews"):
            os.makedirs(os.path.join(rd, sub))
        with open(os.path.join(rd, "DDLSelection.local"), "w", encoding="utf-8", newline="\n") as f:
            f.write("<?xml version = '1.0' encoding = 'UTF-8'?>\n<oracle.dbtools.crest.model.design.relational.RelationalDesign objectID=\"%s\"/>" % self.rel_id)
        with open(os.path.join(rd, "Diagrams.local"), "w", encoding="utf-8", newline="\n") as f:
            f.write("<?xml version = '1.0' encoding = 'UTF-8'?>\n<oracle.dbtools.crest.model.design.relational.RelationalDesign>\n   <object objectType=\"main_view\" objectID=\"%s\" name=\"Relational_1\" visible=\"true\"/>\n</oracle.dbtools.crest.model.design.relational.RelationalDesign>" % self.main_view)
        objs = ["<?xml version = '1.0' encoding = 'UTF-8'?>", "<oracle.dbtools.crest.model.design.relational.RelationalDesign>"]
        for t in TABELAS:
            objs.append('   <object objectType="Table" objectID="%s" name="%s" seqName="seg_0" propertyClassName="oracle.dbtools.crest.model.design.relational.Table" propertyParentId="%s" propertySourceId="" propertyTargetId=""/>' % (
                self.tab_ids[t["nome"]], t["nome"], self.rel_id))
            with open(os.path.join(rd, "table", "seg_0", self.tab_ids[t["nome"]] + ".xml"), "w", encoding="utf-8", newline="\n") as f:
                f.write(self.xml_tabela(t))
        for t in TABELAS:
            for fk in t["fks"]:
                fid = self.fk_ids[(t["nome"], fk["nome"])]
                objs.append('   <object objectType="FKIndexAssociation" objectID="%s" name="%s" containerID="%s" refContainerID="%s" seqName="seg_0" propertyClassName="oracle.dbtools.crest.model.design.relational.FKIndexAssociation" propertyParentId="%s" propertySourceId="%s" propertyTargetId="%s"/>' % (
                    fid, fk["nome"], self.tab_ids[t["nome"]], self.tab_ids[fk["ref"]], self.rel_id, self.tab_ids[fk["ref"]], self.tab_ids[t["nome"]]))
                with open(os.path.join(rd, "foreignkey", "seg_0", fid + ".xml"), "w", encoding="utf-8", newline="\n") as f:
                    f.write(self.xml_fk(t, fk))
        objs.append("</oracle.dbtools.crest.model.design.relational.RelationalDesign>")
        with open(os.path.join(rd, "Objects.local"), "w", encoding="utf-8", newline="\n") as f:
            f.write("\n".join(objs))

        with open(os.path.join(raiz, "domains", self.nome + "_Domains.xml"), "w", encoding="utf-8", newline="\n") as f:
            f.write(self.xml_dominios())

        import diagrama
        lay = layout.calcular(diagrama._medidor(), dm=True)
        with open(os.path.join(rd, "subviews", self.main_view + ".xml"), "w", encoding="utf-8", newline="\n") as f:
            f.write(self.xml_subview(lay))

        # modelo fisico (Oracle) com proxies de tabela e sequences
        ph = os.path.join(rd, "phys", curto(SITE_ID))
        for sub in ("Table/seg_0", "Sequence/seg_0", "User/seg_0", "SType/seg_0"):
            os.makedirs(os.path.join(ph, sub))
        with open(os.path.join(ph, curto(SITE_ID) + ".xml"), "w", encoding="utf-8", newline="\n") as f:
            f.write("<?xml version = '1.0' encoding = 'UTF-8'?>\n"
                    '<StorageDesignOraclev20c class="oracle.dbtools.crest.model.design.storage.oracle.v20c.StorageDesignOraclev20c" name="%s" id="%s" siteID="%s" siteName="%s" siteType="%s">\n' % (SITE_NOME, gid(), SITE_ID, SITE_NOME, SITE_TIPO)
                    + cab(nome) + "</StorageDesignOraclev20c>")
        with open(os.path.join(ph, "DDLSelection.local"), "w", encoding="utf-8", newline="\n") as f:
            f.write("<?xml version = '1.0' encoding = 'UTF-8'?>\n<oracle.dbtools.crest.model.design.storage.oracle.v20c.StorageDesignOraclev20c>\n</oracle.dbtools.crest.model.design.storage.oracle.v20c.StorageDesignOraclev20c>")
        pobjs = ["<?xml version = '1.0' encoding = 'UTF-8'?>", "<oracle.dbtools.crest.model.design.storage.oracle.v20c.StorageDesignOraclev20c>"]
        for t in TABELAS:
            tid = self.tab_ids[t["nome"]]
            pobjs.append('   <object objectType="Table" objectID="%s" name="%s" seqName="seg_0" propertyClassName="oracle.dbtools.crest.model.design.storage.oracle.v20c.TableProxyOraclev20c" propertyParentId="%s"/>' % (tid, t["nome"], curto(SITE_ID)))
            with open(os.path.join(ph, "Table", "seg_0", tid + ".xml"), "w", encoding="utf-8", newline="\n") as f:
                f.write("<?xml version = '1.0' encoding = 'UTF-8'?>\n"
                        '<TableProxyOraclev20c class="oracle.dbtools.crest.model.design.storage.oracle.v20c.TableProxyOraclev20c" name="%s" directorySegmentName="seg_0" id="%s">\n' % (t["nome"], tid)
                        + cab(nome) + "<logging>YES</logging>\n<partitionedRowMovement>DISABLE</partitionedRowMovement>\n<temporaryChanged>false</temporaryChanged>\n"
                        + '<primaryKeys>\n<PrimaryKey class="oracle.dbtools.crest.model.design.storage.oracle.v10g.PKProxyOraclev10g" name="%s" id="%s">\n' % (t["pk"]["nome"], self.pk_ids[t["nome"]])
                        + cab(nome) + "<indexSort>SORTED</indexSort>\n</PrimaryKey>\n</primaryKeys>\n</TableProxyOraclev20c>")
        for s in SEQUENCIAS:
            sid = self.seq_ids[s["nome"]]
            pobjs.append('   <object objectType="Sequence" objectID="%s" name="%s" seqName="seg_0" propertyClassName="oracle.dbtools.crest.model.design.storage.oracle.v10g.SequenceOraclev10g" propertyParentId="%s"/>' % (sid, s["nome"], curto(SITE_ID)))
            with open(os.path.join(ph, "Sequence", "seg_0", sid + ".xml"), "w", encoding="utf-8", newline="\n") as f:
                f.write("<?xml version = '1.0' encoding = 'UTF-8'?>\n"
                        '<SequenceOraclev10g class="oracle.dbtools.crest.model.design.storage.oracle.v10g.SequenceOraclev10g" name="%s" directorySegmentName="seg_0" id="%s">\n' % (s["nome"], sid)
                        + cab(nome) + "<incrementBy>%d</incrementBy>\n<noCache>YES</noCache>\n<startWith>%d</startWith>\n</SequenceOraclev10g>" % (s["increment"], s["start"]))
        pobjs.append("</oracle.dbtools.crest.model.design.storage.oracle.v20c.StorageDesignOraclev20c>")
        with open(os.path.join(ph, "Objects.local"), "w", encoding="utf-8", newline="\n") as f:
            f.write("\n".join(pobjs))
        return dmd


def gerar(pasta_saida, template, nome="TOTVS_Inteligencia_Conversacional"):
    return Design(nome).escrever(pasta_saida, template)
