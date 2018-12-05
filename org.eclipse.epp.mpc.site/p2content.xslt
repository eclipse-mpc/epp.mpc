<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
   <xsl:output method="xml" encoding="utf-8" indent="no"/>

   <xsl:param name="uss-update-site-name">User Storage SDK</xsl:param>
   <xsl:param name="uss-update-site">http://download.eclipse.org/usssdk/updates/release/latest</xsl:param>
   <xsl:param name="uncategorized-category">no.category</xsl:param>
   
   <!-- copy everything by default -->
   <xsl:template match="*">
      <xsl:copy>
         <xsl:apply-templates select="@*|node()"/>
      </xsl:copy>
   </xsl:template>
   <xsl:template match="@*|text()|comment()|processing-instruction">
      <xsl:copy-of select="."/>
   </xsl:template>

   <!-- remove uncategorized category -->
   <xsl:template match="//units/unit[contains(@id, concat('.', $uncategorized-category))]"/>
   <!-- remove potential pre-existing references node: we take care of that ourselves -->
   <xsl:template match="/repository/references"/>
   <!-- reduce units size by 1 due to removed category unit -->
   <xsl:template match="//units/@size">
      <xsl:attribute name="size"><xsl:value-of select="current()-1"/></xsl:attribute>
   </xsl:template>
   <!-- add new references node -->
   <xsl:template match="/repository">
      <xsl:copy>
         <xsl:apply-templates select="@*"/>
         <xsl:text>
</xsl:text>
         <xsl:text>  </xsl:text>
         <xsl:apply-templates select="properties"/>
         <xsl:text>
</xsl:text>
         <xsl:text>  </xsl:text>
         <references size='4'>
         <xsl:text>
</xsl:text>
            <xsl:text>    </xsl:text>
            <repository name='{$uss-update-site-name}' uri='{$uss-update-site}' url='{$uss-update-site}' type='0' options='1'/>
            <xsl:text>
</xsl:text>
            <xsl:text>    </xsl:text>
            <repository name='{$uss-update-site-name}' uri='{$uss-update-site}' url='{$uss-update-site}' type='1' options='1'/>
            <xsl:text>
</xsl:text>
         <xsl:text>  </xsl:text>
         </references>
         <xsl:apply-templates select="node()[not(self::properties)]"/>
      </xsl:copy>
   </xsl:template>
   <!-- control whitespace before root node -->
   <xsl:template match="/">
      <xsl:text>
</xsl:text>
      <xsl:copy>
         <xsl:apply-templates select="@*|node()"/>
      </xsl:copy>
   </xsl:template>
   
</xsl:stylesheet>