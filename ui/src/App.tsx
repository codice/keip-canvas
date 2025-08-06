import {
  Column,
  Grid,
  Header,
  HeaderGlobalBar,
  HeaderName,
} from "@carbon/react"

import "./styles.scss"

import FlowCanvas from "./components/canvas/FlowCanvas"
import EipConfigSidePanel from "./components/config-panel/EipConfigSidePanel"
import Palette from "./components/palette/Palette"
import OptionsMenu from "./components/options-menu/OptionsMenu"
import ToolbarMenu from "./components/toolbar/ToolbarMenu"

const App = () => (
  <>
    <Grid className="main-layout-grid">
      <Column span="100%">
        <Header aria-label="Canvas Title" className="header-main">
          <HeaderName prefix="" className="header-main-text">
            Keip Canvas
          </HeaderName>
          <HeaderGlobalBar className="header-action-bar">
            <OptionsMenu />
          </HeaderGlobalBar>
        </Header>
      </Column>

      <Column>
        <Palette />
      </Column>

      <Column>
        <FlowCanvas />
      </Column>

      <Column>
        <EipConfigSidePanel />
      </Column>

      <Column span="100%">
        <ToolbarMenu />
      </Column>
    </Grid>
  </>
)

export default App
